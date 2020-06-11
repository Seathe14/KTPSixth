import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

public class FractalExplorer {
    private int displaySize;
    private JImageDisplay ImageDisplay;
    private FractalGenerator Fractal;
    private Rectangle2D.Double Range2D;
    private int rowsRemaining;


    private JComboBox<String> comboBox1;
    private JButton resetButton;
    private JButton saveImage;

    // Enable/Disable buttons etc
    private void enableUI(boolean val)
    {
        comboBox1.setEnabled(val);
        resetButton.setEnabled(val);
        saveImage.setEnabled(val);
    }
    //Multithread class
    private class FractalWorker extends SwingWorker<Object,Object>
    {
        int yCoordinate;
        int[] arrRgb;
        // Current pixel row
        FractalWorker(int y)
        {
            yCoordinate = y;
        }
        @Override
        protected Object doInBackground() throws Exception {
           // float hue = 0.7f + (float)iteration / 200f;

            arrRgb = new int[displaySize];
            //Итерации по x координате пикселей, y статичен, т.е строка пикселей
            for(int i =0;i<arrRgb.length;i++) {
                double xCoord = Fractal.getCoord(Range2D.x, Range2D.x + Range2D.width,displaySize,i);
                double yCoord = Fractal.getCoord(Range2D.y, Range2D.y + Range2D.height,displaySize,yCoordinate);
                int iteration = Fractal.numIterations(xCoord,yCoord);
                //Массив цветов в пикселе при y координате, т.е строке
                if(iteration == -1)
                    arrRgb[i] = 0;
                else {
                    float hue = 0.7f + (float)iteration / 200f;
                    arrRgb[i] = Color.HSBtoRGB(hue, 1.0f, 1.0f);
                }
            }
            return null;
        }

        @Override
        protected void done() {
            //Рисует пиксели в каждой колонке i пикселя текущей y строки
            for(int i =0; i < arrRgb.length ;i++)
            {
                ImageDisplay.drawPixel(i,yCoordinate,arrRgb[i]);

            }
            ImageDisplay.repaint(0,0,yCoordinate,displaySize,1);
            rowsRemaining--;
            if(rowsRemaining == 0)
                enableUI(true);
          //  super.done();
        }
    }



    public FractalExplorer(int DisplaySize) {
        displaySize = DisplaySize;
        // Creating image display
        ImageDisplay = new JImageDisplay(displaySize, displaySize);
        // Creating reference to base object
        Fractal = new Tricorn();
        Range2D = new Rectangle2D.Double(0, 0, displaySize, displaySize);
        Fractal.getInitialRange(Range2D);
    }

    public void createAndShowGui() {
        ImageDisplay.setLayout(new BorderLayout());
        // Creating Window
        JFrame Frame = new JFrame("Fractal Explorer");
        Frame.add(ImageDisplay, BorderLayout.CENTER);

        //Adding combobox to Ui
        comboBox1 = new JComboBox<String>();
        comboBox1.addItem("Tricorn");
        comboBox1.addItem("Mandelbrot");
        comboBox1.addItem("Something");
        comboBox1.addItem("Burning Ship");
        ComboBoxHandler comboBoxHandler = new ComboBoxHandler();
        comboBox1.addActionListener(comboBoxHandler);
        //Frame.add(comboBox1,BorderLayout.NORTH);
        //Adding label to UI
        JLabel cmbBoxLbl = new JLabel("Fractal:");
      //  Frame.add(cmbBoxLbl,BorderLayout.NORTH);


        //Creating Jpanel
        JPanel panel1 = new JPanel();
        panel1.add(cmbBoxLbl);
        panel1.add(comboBox1);
        Frame.add(panel1,BorderLayout.NORTH);


        // Button Reset position and Event Handler
        resetButton = new JButton("Reset");
        ButtonHandler buttonHandler = new ButtonHandler();
        resetButton.addActionListener(buttonHandler);
        Frame.add(resetButton, BorderLayout.SOUTH);
        // Button save image
        saveImage = new JButton("Save Image");
        saveImage.addActionListener(buttonHandler);


        // Jpanel for buttons
        JPanel panel2 = new JPanel();
        panel2.add(saveImage);
        panel2.add(resetButton);
        Frame.add(panel2,BorderLayout.SOUTH);



        Frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // MouseHandler
        MouseHandler click = new MouseHandler();
        ImageDisplay.addMouseListener(click);

        Frame.pack();
        Frame.setVisible(true);
        Frame.setResizable(false);
    }
    private void drawFractal()
    {
        // Выключаем UI
        enableUI(false);
        // Количество строк равно числу y, в нашем случае displaySize
        rowsRemaining = displaySize;
        //Вызываем конструктор FractalWorker'a и присваиваем ему текущую строку
        //И запускаем фоновый поток
        for(int x = 0;x<displaySize;x++)
        {
            FractalWorker fractalWorker = new FractalWorker(x);
            fractalWorker.execute();
        }

    }
    private class ComboBoxHandler implements  ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e) {
            JComboBox comboBox = (JComboBox)e.getSource();
            String command = (String)comboBox.getSelectedItem();
            switch(command) {
                case "Tricorn":
                    Fractal = new Tricorn();
                    break;
                case "Mandelbrot":
                    Fractal = new Mandelbrot();
                    break;
                case "Something":
                    Fractal = new Something();
                    break;
                case "Burning Ship":
                    Fractal = new BurningShip();
                    break;
            }
            Fractal.getInitialRange(Range2D);
            drawFractal();
        }
    }
    private class ButtonHandler implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            String command = e.getActionCommand();
            if (command.equals("Reset")) {
                Fractal.getInitialRange(Range2D);
                drawFractal();
            }
            if(command.equals("Save Image"))
            {
                JFileChooser fileChooser = new JFileChooser();
                FileFilter filter = new FileNameExtensionFilter("PNG Images","png");
                fileChooser.setFileFilter(filter);
                fileChooser.setAcceptAllFileFilterUsed(false);
                //fileChooser.showSaveDialog(ImageDisplay);
                if(fileChooser.showSaveDialog(ImageDisplay)!=JFileChooser.APPROVE_OPTION )
                    return;
                File file = fileChooser.getSelectedFile();
                try{
                    ImageIO.write(ImageDisplay.BufImage,"png",new File(file.toString() + ".jpg"));
                } catch (IOException exc)
                {
                    JOptionPane.showMessageDialog(ImageDisplay,exc.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
                }

            }
        }
    }
    private class MouseHandler extends MouseAdapter
    {
        @Override
        public void mouseClicked(MouseEvent e)
        {
            if(rowsRemaining!=0)
                return;
            int x = e.getX();
            int y = e.getY();
            double xCoord = Fractal.getCoord(Range2D.x, Range2D.x + Range2D.width, displaySize,x);
            double yCoord = Fractal.getCoord(Range2D.y, Range2D.y + Range2D.height, displaySize,y);
            Fractal.recenterAndZoomRange(Range2D,xCoord,yCoord,0.5);
            drawFractal();
        }
    }
    public static void main(String[] args)
    {
        FractalExplorer FE = new FractalExplorer (800);
        FE.createAndShowGui();
        FE.drawFractal();
    }
}
