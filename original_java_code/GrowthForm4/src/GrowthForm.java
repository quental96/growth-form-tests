import javax.imageio.ImageIO;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLJPanel;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

public class GrowthForm implements Runnable {

   private ButtonGroup bGroup3 = new ButtonGroup();
   private ButtonGroup bGroup2 = new ButtonGroup();
   static Thread displayT = new Thread(new GrowthForm());
   volatile Form form;
   int regionR = 6;           //region radius from center for storage and spring calcs.
   GLCanvas canvas = new GLCanvas();
   DisplayInfo displayInfo = null;    //for display and stl 
   Random rnd = new Random();
   //int nGrowers = 1;          //desired number
   double alpha = 0.3;          //bump height
   double beta = 0.2;           //bump radius
   double gamma = 0.2;          //bump shoulder height
   //int growSize = 2;          //initial size of growth area
   private int nGrow;         //#steps to grow after button pushed
   Bud nextBud;               //the one which will divide a cell next
   private boolean displayReady=false;
   private boolean okFlag=false;
   private boolean scriptMode;  //script or interactive?
   private boolean resetFlag = false;
   private boolean stlFlag = false;
   final static int REGION_HOPS = 4;  //how big is the local region?
   Parser[] ps;                 //parsed script lines 
   Robot robot;                 //for video capture
   boolean captureMode;
   int nScreen = 0;
   int nScreenInterval = 10;
   float spinAngle = 0.0f;      //auto spin
   float spinRate = 0.01f;
   
   //GUI objects that we need to reference in multiple places
   JFrame frame; 
   JLabel status;
   JCheckBox showNextCheckBox;
   JTextField command;
   JTextField index;
   JTextField type;
   JTextField textNGrow;  
   JRadioButton colorByRegionButton;
   JRadioButton colorByAssignedButton;
   JRadioButton colorByNButton;
   JRadioButton colorByFreezeButton;
   JRadioButton colorBySizeButton;
   ButtonGroup bGroup1 = new ButtonGroup();
   JSlider alphaSlider;
   JLabel alphaLabel;
   JSlider betaSlider;
   JLabel betaLabel;
   JSlider gammaSlider;
   JLabel gammaLabel;
   JFileChooser stlFileChooser = new JFileChooser();
   JFileChooser gfsFileChooser = new JFileChooser();
   JProgressBar progressBar;
   JTextPane script;
   JTextField nScript;
   JButton startScriptButton;
   JButton continueButton;
   JButton stopButton;
   JLabel scriptStatusLabel;
   JRadioButton trianglesRadioButton;
   JRadioButton assignedRadioButton;
   JRadioButton solidTextureRadioButton;
   JRadioButton radioButton0;
   JRadioButton radioButton1;
   JRadioButton radioButton2;
   JRadioButton radioButton3;
   JCheckBox blendColorCheckBox;
   JCheckBox blendTextureCheckBox;
   JCheckBox checkBoxVideo;
   JSlider spinSlider;
   
   public static void main(String[] args) {
      displayT.start();
   }

   public void run() { 
      makeGUI();
      try {
         robot = new Robot();
      } catch (AWTException e1) {
         e1.printStackTrace();
      }
      ActionListener viewer = new ActionListener(){
         public void actionPerformed(ActionEvent e){
            if (displayReady) canvas.display();
            state(form.cell.size() + " cells" + form.budCountString());
         }
      };
      new Timer(33, viewer).start();       //start animation and GL display

      scriptMode = false;      // running from the buttons, not script
      initForm();       
      Bud nextBud = null;      // holds bud that grows next
      nGrow = 0;               //# steps still to grow after button pushed

      while(true){                        // MAIN LOOP:
         form.center();   

         if (nextBud == null && nGrow>0){       //First choose next bud to act                  
            if (scriptMode)
               nextBud = form.chooseNextToDivide();   //balance between all budTypes...
            else {
               nextBud = guiBud();                    //...or user-selected bud
               if (nextBud!=null) nextBud.chooseCell();
            }
         }

         if (scriptMode && nGrow > 0) {                     //Then do one action
            if (nextBud != null){
               nextBud.act(ps, this);
               nGrow--;
               nextBud = null;
            }
         }
         else if ((!showNextCheckBox.isSelected() || okFlag) && nGrow>0 && nextBud!=null){
            nextBud.divideChosenCell();
            nGrow--;
            okFlag=false;
            nextBud=null;
         }       
         relax();                           //jiggle xyz's

         if (resetFlag){
            if (scriptMode) initScript(); else initForm();
            nextBud = null;
            resetFlag = false;
         }     
         
         maybeGenerateGraphics();            //prepare xyz and color daya for display
         
         if (stlFlag){                       //stop loop to output file
            exportBinarySTL();
            stlFlag = false;
         }
         
         maybeCapture();                      //make video

         if (scriptMode && nGrow==0) continueButton.setEnabled(true);
      }
   }
   
   //generate frame of video every nth division
   public void maybeCapture(){
      if (captureMode && nScreen%nScreenInterval == 0) capture("growth", nScreen/nScreenInterval);
      nScreen++;     
   }
   
   //generate frame, but skip if in video mode and frame is being skipped
   void maybeGenerateGraphics(){
      form.cleanup();
      spinAngle += getSpinRate();     //rotation
      if (captureMode && nScreen%nScreenInterval != 0) return;
      Color[] dispColor;     //colors for cells    
      if (colorByAssignedButton.isSelected()) dispColor = form.colorAllByAssigned(); 
      else if (colorBySizeButton.isSelected()) dispColor = form.colorAllByRadius(); 
      else if (colorByFreezeButton.isSelected()) dispColor = form.colorAllBySterile(); 
      else if (colorByNButton.isSelected())dispColor =  form.colorAllByN();
      else dispColor = form.colorAllByGrowers(Color.RED, Color.YELLOW);
      if (blendColorCheckBox.isSelected()) dispColor = form.blendColor(dispColor);     
      if (showNextCheckBox.isSelected() && nextBud!=null) 
         dispColor[nextBud.getChoosenCell().index] = Color.WHITE;

      if (trianglesRadioButton.isSelected())
         displayInfo = makeTriangles(dispColor);   //for display and stl export
      else
      {
         Texture[] dispTexture;
         if (solidTextureRadioButton.isSelected())
            dispTexture = form.getSolidTexture(alpha, beta, gamma);
         else
            dispTexture = form.getAssignedTexture();
         if (blendTextureCheckBox.isSelected()) dispTexture = form.blendTexture(dispTexture); 
         displayInfo = makeTexture(dispColor, dispTexture);
      }
   }
   
   public float getSpinAngle(){
      return spinAngle;
   }
   
   public void initForm(){         //square pyramid = triangular dipyramid
         int[][] verts=new int[][] {{0,0,1}, {1,0,0}, {0,1,0}, {-1,0,0}, {0,-1,0}};
         int[][] adj = new int[][] {{1,2,3,4}, {0,4,3,2}, {0,1,3}, {0,2,1,4}, {0,3,1}};
         form = new Form(verts, adj, REGION_HOPS);          
         form.sphericalize();
         //for (Cell v : form.cell) v.setRadiusDontDiffuse(cellRadius);  //current radius
         BudType budTypeA = new BudType("A");            //one bud of type A
         form.add(budTypeA);
         budTypeA.createBud(form.cell.get(0));
//         BudType budTypeB = new BudType("B");            //one bud of type B
//         form.add(budTypeB);
//         budTypeB.createBud(form.cell.get(1)); 
//         budTypeB.getBuds().get(0).fatness=1;
   }
   
   //relax some places    TODO: pick high stress ones?
   public void relax(){
      for (int i=0; i<50; i++){
         int j = rnd.nextInt(form.cell.size());
         form.springXYZ(form.cell.get(j));
      }
   }

   public DisplayInfo getDisplayInfo() {
          return displayInfo;
   }
   
   //sleep for n milliseconds
   public void sleep(int n) {
       try {Thread.sleep(n);}
       catch (Exception e) {} 
   }
   
   void state(String str){              // display status
      status.setText("  " + str);
      scriptStatusLabel.setText("  " + str);
    }
   
   ///////////////////// Command Line Parsing and Script Execution ////////////////////
   
   //word is flagged with T if followed by BudType or n if followed by n integers
   //or U if followed by one budtype then 1 integer
   final String[] COMMANDS = {"1grow", "1sleep", "1fatness", "1size", "1line",
        "Tring", "Tfill", "Tspawn", "Ttrail", "Tbecome", "Trepel",
        "0noop", "0freeze", "0larger", "0smaller", "0inwards", "0outwards", "0die",
        "0upwards", "0downwards", "0radial", "0nocollisioncheck", "1general",
        "0flat", "0spike", "0bump", "0web", "0hairy", "3texture", "3color",
        "3mustface", "3headtowards", "1blob", "Udisperse", "0tube"};
     
   //a bud action for each legal command, except grow is handled separately
   void doParsedCommand(Parser p, Bud b){
      String cmdWord = p.getCommandWord();
      if (cmdWord.equals("noop")) ;  //(no operation, e.g. blank line)
      else if (cmdWord.equals("blob")) b.makeBlob(p.getArg1(), this);
      else if (cmdWord.equals("disperse")) b.disperse(p.getBudType(), p.getArg1(), this);
      else if (cmdWord.equals("freeze")) b.frozen=true;
      else if (cmdWord.equals("die"))  b.die();
      else if (cmdWord.equals("larger"))  b.larger(0.1);
      else if (cmdWord.equals("smaller")) b.larger(-0.1);
      else if (cmdWord.equals("inwards")) b.inwards();
      else if (cmdWord.equals("outwards")) b.outwards();
      else if (cmdWord.equals("upwards")) b.towards(new XYZ(0,0,1));
      else if (cmdWord.equals("downwards")) b.towards(new XYZ(0,0,-1));
      else if (cmdWord.equals("headtowards")) b.towards(new XYZ(p.getArg1(), p.getArg2(), p.getArg3()));
      else if (cmdWord.equals("radial")) b.towards(b.cell.xyz);
      else if (cmdWord.equals("mustface")) b.mustface(new XYZ(p.getArg1(), p.getArg2(), p.getArg3()));
      else if (cmdWord.equals("nocollisioncheck")) b.setCollisionCheck(false);
      else if (cmdWord.equals("fatness")) b.fatness = p.getArg1();
      else if (cmdWord.equals("size")) b.setSize(p.getArg1()/20.0);  //10=default size
      else if (cmdWord.equals("ring")) b.ring(p.getBudType(), 2);
      else if (cmdWord.equals("fill")) b.fill(p.getBudType());
      else if (cmdWord.equals("spawn")) b.spawn(p.getBudType(), false);
      else if (cmdWord.equals("trail")) b.spawn(p.getBudType(), true);
      else if (cmdWord.equals("become")) b.become(p.getBudType());
      else if (cmdWord.equals("repel")) b.repel(p.getBudType());
      else if (cmdWord.equals("line")) b.formLine(p.getArg1());
      else if (cmdWord.equals("flat")) b.cell.setTextureFlat();
      else if (cmdWord.equals("bump")) b.cell.setTextureBump();
      else if (cmdWord.equals("spike")) b.cell.setTextureSpike();
      else if (cmdWord.equals("web")) b.cell.setTextureWeb();
      else if (cmdWord.equals("hairy")) b.cell.setTextureHairy();
      else if (cmdWord.equals("texture")) b.setBudTexture(p.getArg1(), p.getArg2(), p.getArg3());
      else if (cmdWord.equals("color")) b.setBudColor(p.getArg1(), p.getArg2(), p.getArg3());
      else if (cmdWord.equals("tube")) b.tube();
          else report("Parsing bug: " + cmdWord);
   }
   
   //interactive button clicked.  execute one typed line
   void doExecute(){
      scriptMode = false;
      Bud b = guiBud();          //user specifies the bud
      if (b==null) {
         Toolkit.getDefaultToolkit().beep();
         return;
      }
      Parser p = new Parser(command.getText(), COMMANDS, form.budTypes);
      //create  bud types when they are needed:
      if (!p.isLegal() && p.getErrorExplanation().substring(0, 15).equals("Not a BudType: ")){
         String name = p.getErrorExplanation().substring(15);
         BudType bt = new BudType(name);
         form.add(bt);
         report("Autocreated BudType: " + name);
         p = new Parser(command.getText(), COMMANDS, form.budTypes);
      }
      if (!p.isLegal()){
         Toolkit.getDefaultToolkit().beep();      //not a command
         report(p.getErrorExplanation());
         return;
      } 
      doParsedCommand(p, b);
   }

    
   //return the bud that the GUI describes, e.g., A0, or null if none
   private Bud guiBud() {
      Integer idx;
      try {
         idx = Integer.parseInt(index.getText());
      } catch (NumberFormatException e) {
         return null;
      }
      for (BudType bt : form.budTypes)
         if (bt.matchName(type.getText())){
            if (idx<0 || idx>=bt.getBuds().size()) return null;
            return bt.getBuds().get(idx);
         }
      return null;
   }

   private void initScript(){
      int initialSize = 4;          //for tetrahedron
      int initialBuds = 1;          //one initial bud
      Integer nToGrow = getN();     //value from text box
      if (nToGrow == null) return;
      nGrow=0;
      startScriptButton.setEnabled(false);
      continueButton.setEnabled(false);
      stopButton.setEnabled(true);
      
      int[][] verts=new int[][] {{1,1,1}, {-1,1,-1}, {-1,-1,1}, {1,-1,-1}}; //tetrahedron
      int[][] adj = new int[][] {{1,2,3}, {0,3,2}, {0,1,3}, {0,2,1}};
      form = new Form(verts, adj, REGION_HOPS);   //create form to hold budTypes
      form.sphericalize();  
      
      String scriptStr = script.getText();            //script from GUI textbox
      scriptStr = scriptStr.replace(";", "\n");       //treat semi as line delimiter
      scriptStr = scriptStr.replace(",", "\n");       //treat comma as line delimiter
      scriptStr = scriptStr.replace("red",   "color 255 0 0"); 
      scriptStr = scriptStr.replace("green", "color 0 255 0"); 
      scriptStr = scriptStr.replace("blue",  "color 0 0 255"); 
      scriptStr = scriptStr.replace("yellow","color 255 255 0"); 
      scriptStr = scriptStr.replace("orange","color 255 128 0"); 
      scriptStr = scriptStr.replace("purple","violet"); 
      scriptStr = scriptStr.replace("violet","color 255 0 255"); 
      scriptStr = scriptStr.replace("white", "color 255 255 255"); 
      scriptStr = scriptStr.replace("black", "color 32 32 32"); 
      scriptStr = scriptStr.replace("gray",  "color 128 128 128"); 
      String[] line = scriptStr.split("\\n");         //Parse script
      
      //check for special first line syntax
      Parser firstLine = new Parser(line[0], new String[] {"2start"}, form.budTypes);
      if (firstLine.isLegal() && firstLine.getCommandWord().equals("start")){
         initialSize = firstLine.getArg1();                  //"start n m"  n cells, m buds
         initialBuds = firstLine.getArg2();
         if (initialSize < initialBuds) {
            report("Can't have more buds than cells!");
            stopScript();
            return;
         }
         line[0] = "";                         //hide the start line from remainder of parser
      }
      
      BudType previousBT = null;
      for (int i=0; i<line.length; i++){                     //1st pass.  Find BudType defns
         String name = Parser.wordColon(line[i]);
         if (name != null){
            BudType bt = new BudType(name);
            form.add(bt);
            if (previousBT != null) previousBT.stepEnd = i;
            //if (previousBT==null && i!=0) report("Warning: first " + i + " lines ignored.");
            bt.stepStart = i+1;                              //note where they start
            previousBT = bt;
         }       
      } 
      if (previousBT != null) previousBT.stepEnd = line.length;  // end of last one
      if (previousBT == null) report("Warning: no definitions in script");
      
      ps = new Parser[line.length];                              //2nd pass
      for (int i=0; i<line.length; i++){
         ps[i] = new Parser(line[i], COMMANDS, form.budTypes);
         if (!ps[i].isLegal()){
            report("Script error, line "+(i+1)+": "+ps[i].getErrorExplanation());
            stopScript();
            return;
         }
      }
      if (form.budTypes.size() > 0){          //initial blob
         BudType bt = form.budTypes.get(0);   //1st budtype in script
         bt.createBud(form.cell.get(0));      //create one in cell 0
         Bud bud = bt.getBuds().get(0);       //use it
         for (int i = 0; i<initialSize-4; i++){
            bud.chooseGeneralVertex();
            bud.divideChosenCell();
            relax();
            maybeGenerateGraphics();
         }
         for (int i=1; i<initialBuds; i++)        //create the rest
            bt.createBud(form.cell.get(i));
         for (int repeat=0; repeat<10; repeat++)  //spread them out
            for (Bud b : bt.getBuds()){
               b.repel(bt);
               relax();
            }
      }
      nGrow = nToGrow; 
   }
   
   private void startScript(){             //button clicked
      int response;
      if (checkBoxVideo.isSelected()){
         String ansText = JOptionPane.showInputDialog(frame, "Record every nth frame", "5");
         try {
            response = Integer.parseInt(ansText);
         } catch (NumberFormatException e) {
            response = -1;
         }
         if (response > 0){
            nScreen=0;
            nScreenInterval = response;
            captureMode = true;
         }
      }
      scriptMode = true;
      resetFlag = true;
   }
   
   private void continueScript(){          //button clicked
      startScriptButton.setEnabled(false);
      continueButton.setEnabled(false);
      stopButton.setEnabled(true);
      if (getN() == null) return;
      nGrow = getN();
   }
   
   private void stopScript(){              //button clicked
      startScriptButton.setEnabled(true);
      continueButton.setEnabled(true);
      stopButton.setEnabled(false);
      nGrow = 0;
      captureMode = false;
   }
   
   //get number from text box, or null if not a number
   Integer getN(){
      Integer ans = null;
      try {
         ans = Integer.parseInt(nScript.getText().trim());
      } catch (NumberFormatException e) {
         Toolkit.getDefaultToolkit().beep(); 
         return ans;
      }
      return ans;
   }
   
   ////////////////////////////// Triangle Mesh methods //////////////////////////

   //create triangles from cell adjacency info
   public DisplayInfo makeTriangles(Color[] dispColor) {
      synchronized(form){
         XYZ[] vSmooth = form.makeSmooth(nSmooth());
         float[][] vert = new float[form.cell.size()][3];     //cell xyz coords
         float[][] col = new float[form.cell.size()][3]; //3 rgb colors per cell
         int[] tri = new int[3*form.countFaces()];         //in groups of 3
         int[] line = new int[2*form.countEdges()];        //in pairs  TODO
         
       int i=0;                             //index the points
       for (XYZ v : vSmooth){  
          vert[i][0] = (float) v.x;
          vert[i][1] = (float) v.y;
          vert[i][2] = (float) v.z;
          i++;
       }

       int ii=0;                             //their colors
       for (Color c : dispColor)  
          col[ii++] = c.getColorComponents(null);
       
         int j = 0;                           //index the triangles
         int k = 0;                           //index the lines
         for (Cell v1 : form.cell){
            if (v1.adj.size()<3) 
               throw new IllegalArgumentException("Bug 4");
            Cell v2 = v1.adj.get(v1.adj.size()-1);   //start with last one for wrap-around
            for (Cell v3 : v1.adj){
               if (v1.index < v2.index  && v1.index < v3.index){     //elim. dupl.
                  tri[j++] = v1.index;
                  tri[j++] = v2.index;
                  tri[j++] = v3.index;
                  if (v1.index < v2.index) {line[k++]=v1.index; line[k++]=v2.index;}  //2 pts per line
                  if (v2.index < v3.index) {line[k++]=v2.index; line[k++]=v3.index;} 
                  if (v3.index < v1.index) {line[k++]=v3.index; line[k++]=v1.index;} 
               }
               v2 = v3;                              //v2 always just before v3
            }
         }
         DisplayInfo ans = new DisplayInfo();         //pack it up to go
         ans.vert = vert;  
         ans.col = col;
         ans.tri = tri;
         ans.line = line;
         displayReady=true;
         return ans;
      }
   }
   
   //create bumpy dual texture from cell adjacency info
   //alpha = bump height, beta = radius, gamma = height at radius
   //13 vertices per bump if 6 triangles per edge
   public DisplayInfo makeTexture(Color[] dispColor, Texture[] dispTexture) {
      synchronized(form){
         XYZ[] vSmooth = form.makeSmooth(nSmooth());
         int numPts = form.countVertices() + 6*form.countFaces();
         float[][] vert = new float[numPts][3];     //cell xyz coords
         float[][] col = new float[numPts][3];   //3 rgb colors per cell
         int[] tri = new int[3*6*form.countEdges()];            //in groups of 3
         int[] line = new int[0];                             //no lines
         Form f = new Form(new int[0][0], new int[0][0], 0);  //no topology, just holds points
         int j=0;                                             //j counts the new vertices
         int t=0;                                             //t counts the new triangles
         
         for (Cell v1 : form.cell){                       //make bump around v1
            Cell c = new Cell(f, XYZ.plus(vSmooth[v1.index], XYZ.scale(dispTexture[v1.index].alpha, v1.normal())));  //top center of bump
            c.setColor(dispColor[v1.index]);
            Cell[] p = new Cell[v1.adj.size()];  //points at centers of surrounding triangles
            int ctr=0;                                //ctr counts the number of adjacent v's
            Cell v2 = v1.adj.get(v1.adj.size()-1);   //start with last one for wrap-around
            for (Cell v3 : v1.adj){
               p[ctr] = new Cell(f, XYZ.triCenter(vSmooth[v1.index], vSmooth[v2.index], vSmooth[v3.index]));
               p[ctr].setColor(Color.BLACK);
               ctr++;
               v2 = v3;                                //v2 always just before v3
            }
            Cell[] q = new Cell[v1.adj.size()];  //points a bit closer in than p
            XYZ liftQ = XYZ.scale(dispTexture[v1.index].gamma, v1.normal());
            for (int k=0; k<ctr; k++) {
               q[k] = new Cell(f, XYZ.plus(XYZ.interpolate(dispTexture[v1.index].beta, vSmooth[v1.index], p[k].xyz), liftQ));
               q[k].setColor(dispColor[v1.index].darker());
            }

//This is the previous code, before smoothing (faster)         
//         for (Cell v1 : form.cell){                       //make bump around v1
//            Cell c = new Cell(f, XYZ.plus(v1.xyz, XYZ.scale(alpha, v1.normal())));  //top center of bump
//            c.setColor(v1.getColor());
//            Cell[] p = new Cell[v1.adj.size()];  //points at centers of surrounding triangles
//            int ctr=0;                                //ctr counts the number of adjacent v's
//            Cell v2 = v1.adj.get(v1.adj.size()-1);   //start with last one for wrap-around
//            for (Cell v3 : v1.adj){
//               p[ctr] = new Cell(f, XYZ.triCenter(v1.xyz, v2.xyz, v3.xyz));
//               p[ctr].setColor(Color.BLACK);
//               ctr++;
//               v2 = v3;                                //v2 always just before v3
//            }
//            Cell[] q = new Cell[v1.adj.size()];  //points a bit closer in than p
//            XYZ liftQ = XYZ.scale(gamma, v1.normal());
//            for (int k=0; k<ctr; k++) {
//               q[k] = new Cell(f, XYZ.plus(XYZ.interpolate(beta, v1.xyz, p[k].xyz), liftQ));
//               q[k].setColor(v1.getColor().darker());
//            }
            
                         //Now pack them as c, p[0], p[1], ..., q[0], ...  starting at j.    
            int C=j;     //Hold C to reference them:  c at C,  p[k] at P=C+1+k, q[k] at Q=C+1+ctr+k 
            int P=C+1;
            int Q=P+ctr;
            {vert[j][0]=(float)c.xyz.x;      vert[j][1]=(float)c.xyz.y;      vert[j][2]=(float)c.xyz.z;  
               col[j] = c.color.getComponents(null); j++; }
            for (int k=0; k<ctr; k++)
            {vert[j][0]=(float)p[k].xyz.x;   vert[j][1]=(float)p[k].xyz.y;   vert[j][2]=(float)p[k].xyz.z;  
            col[j] = p[k].color.getComponents(null); j++; }
                      for (int k=0; k<ctr; k++)
            {vert[j][0]=(float)q[k].xyz.x;   vert[j][1]=(float)q[k].xyz.y;   vert[j][2]=(float)q[k].xyz.z;  
            col[j] = q[k].color.getComponents(null); j++; }
                               for (int k=0; k<ctr; k++){                //18 facets per 6-sided bump
               tri[t++]=C;              tri[t++]=Q+k;   tri[t++] = Q+((k+1)%ctr);   //inner tri
               tri[t++]=Q+k;            tri[t++]=P+k;   tri[t++] = Q+((k+1)%ctr);   //middle
               tri[t++]=Q+((k+1)%ctr);  tri[t++]=P+k;   tri[t++] = P+((k+1)%ctr);   //outer

            }
            int z=0; z++;
         }
         DisplayInfo ans = new DisplayInfo();         //pack it up to go
         ans.vert = vert;  
         ans.col = col;
         ans.tri = tri;
         ans.line = line;
         return ans;
      }
   }
  

   ///////////////////////////////// GUI  /////////////////////////////////////
   

  private void makeGUI() {
     frame = new JFrame("Growth and Form"); 
     frame.setSize(739, 668);
     JFrame.setDefaultLookAndFeelDecorated(true); 
     frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  //kill threads when done 
     Container contentPane = frame.getContentPane();
     contentPane.setLayout(new BorderLayout());
     
     JPanel controlPanel = new JPanel();
     controlPanel.setPreferredSize(new Dimension(350,800));
     contentPane.add(controlPanel, BorderLayout.EAST);
     controlPanel.setLayout(new GridLayout(0,1));

     final JTabbedPane tabbedPane = new JTabbedPane();
     tabbedPane.setPreferredSize(new Dimension(0, 500));
     tabbedPane.setMinimumSize(new Dimension(0, 500));
     controlPanel.add(tabbedPane);

     final JPanel panelInteractive = new JPanel();
     panelInteractive.setLayout(new GridLayout(0, 1));
     tabbedPane.addTab("Interactive", null, panelInteractive, null);

     final JPanel panelButtons = new JPanel();
     final GridBagLayout gridBagLayout_2 = new GridBagLayout();
     gridBagLayout_2.rowHeights = new int[] {0,7};
     panelButtons.setLayout(gridBagLayout_2);
     panelInteractive.add(panelButtons);

     final JButton growButton = new JButton();
     growButton.addActionListener(new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
                                               nGrow = Integer.parseInt(textNGrow.getText()); }
     });
     growButton.setText("Grow");
     final GridBagConstraints gridBagConstraints_6 = new GridBagConstraints();
     gridBagConstraints_6.insets = new Insets(0, 0, 0, 10);
     gridBagConstraints_6.anchor = GridBagConstraints.EAST;
     panelButtons.add(growButton, gridBagConstraints_6);

     textNGrow = new JTextField();
     textNGrow.setText("100");
     final GridBagConstraints gridBagConstraints_7 = new GridBagConstraints();
     gridBagConstraints_7.ipadx = 35;
     gridBagConstraints_7.insets = new Insets(0, 0, 0, 0);
     panelButtons.add(textNGrow, gridBagConstraints_7);

     showNextCheckBox = new JCheckBox();
     showNextCheckBox.setText("Show Next");
     final GridBagConstraints gridBagConstraints_9 = new GridBagConstraints();
     gridBagConstraints_9.gridy = 1;
     gridBagConstraints_9.gridx = 0;
     panelButtons.add(showNextCheckBox, gridBagConstraints_9);

     final JButton okButton = new JButton();
     okButton.addActionListener(new ActionListener() {
        public void actionPerformed(final ActionEvent e)  {okFlag=true; if (nGrow==0) nGrow=1;}   //set when click OK button 
     });
     okButton.setText("OK");
     final GridBagConstraints gridBagConstraints_10 = new GridBagConstraints();
     gridBagConstraints_10.gridy = 1;
     gridBagConstraints_10.gridx = 1;
     panelButtons.add(okButton, gridBagConstraints_10);

     final JButton restartButton = new JButton();
     restartButton.addActionListener(new ActionListener() {
        public void actionPerformed(final ActionEvent arg0) {resetFlag=true;}
     });
     restartButton.setText("Restart");
     final GridBagConstraints gridBagConstraints_5 = new GridBagConstraints();
     gridBagConstraints_5.gridx = 0;
     gridBagConstraints_5.gridy = 2;
     gridBagConstraints_5.insets = new Insets(5, 15, 3, 20);
     panelButtons.add(restartButton, gridBagConstraints_5);
     
     JPanel panelControlOptions = new JPanel();
     panelControlOptions.setLayout(new FlowLayout());
     panelInteractive.add(panelControlOptions);

     type = new JTextField();
     type.setToolTipText("Bud type for command");
     type.setText("A");
     panelControlOptions.add(type);

     index = new JTextField();
     index.setToolTipText("Bud index for command");
     index.setText("0");
     panelControlOptions.add(index);

     command = new JTextField();
     command.setToolTipText("Command to execute");
     command.setText("grow 100");
     panelControlOptions.add(command);

     JButton doButton = new JButton();
     doButton.addActionListener(new ActionListener() {
        public void actionPerformed(final ActionEvent arg0) {  doExecute(); }
     });
     doButton.setToolTipText("Execute command");
     doButton.setText("Do");
     panelControlOptions.add(doButton);

     status = new JLabel(" Initializing...");
     panelInteractive.add(status);
 
     final JPanel panelScript = new JPanel();
     panelScript.setLayout(new BorderLayout());
     tabbedPane.addTab("Script", null, panelScript, null);

     final JPanel topPanel = new JPanel();
     panelScript.add(topPanel, BorderLayout.NORTH);

     JButton readFileButton = new JButton();
     readFileButton.addActionListener(new ActionListener() {
        public void actionPerformed(final ActionEvent arg0) { readScript();}
     });
     readFileButton.setText("Open File");
     topPanel.add(readFileButton);

     final JLabel label = new JLabel();
     label.setText("   ");
     topPanel.add(label);

     JButton writeFileButton = new JButton();
     writeFileButton.addActionListener(new ActionListener() {
        public void actionPerformed(final ActionEvent e) { writeScript(); }
     });
     writeFileButton.setText("Save File");
     topPanel.add(writeFileButton);

     script = new JTextPane();
     JScrollPane  scrollPane = new JScrollPane(script);
     panelScript.add(scrollPane, BorderLayout.CENTER);
     
     final JPanel panelView = new JPanel();
     panelView.setMinimumSize(new Dimension(0, 500));
     panelView.setLayout(new GridLayout(0, 1));
     tabbedPane.addTab("View", null, panelView, null);

     final JPanel panelTop = new JPanel();
     panelView.add(panelTop);

     final JLabel colorByLabel = new JLabel();
     panelTop.add(colorByLabel);
     colorByLabel.setText("Color by:  ");

     final JPanel panelColor = new JPanel();
     panelTop.add(panelColor);
     panelColor.setLayout(new GridLayout(0, 1));
     panelColor.setMinimumSize(new Dimension(0, 200));
   
     colorByRegionButton = new JRadioButton();
     colorByRegionButton.setSelected(true);
     bGroup1.add(colorByRegionButton);
     colorByRegionButton.setText("Region");
     panelColor.add(colorByRegionButton);

     colorByAssignedButton = new JRadioButton();
     bGroup1.add(colorByAssignedButton);
     colorByAssignedButton.setText("Assigned");
     panelColor.add(colorByAssignedButton);

     colorByNButton = new JRadioButton();
     bGroup1.add(colorByNButton);
     colorByNButton.setText("Neighbors");
     panelColor.add(colorByNButton);

     colorBySizeButton = new JRadioButton();
     bGroup1.add(colorBySizeButton);
     colorBySizeButton.setText("Size");
     panelColor.add(colorBySizeButton);

     colorByFreezeButton = new JRadioButton();
     bGroup1.add(colorByFreezeButton);
     colorByFreezeButton.setText("Frozen");
     panelColor.add(colorByFreezeButton);

     blendColorCheckBox = new JCheckBox();
     blendColorCheckBox.setText("Blend");
     panelTop.add(blendColorCheckBox);

     final JPanel panelMiddle = new JPanel();
     panelMiddle.setLayout(new GridLayout(0, 1));
     panelView.add(panelMiddle);

     final JPanel panelSmooth = new JPanel();
     panelMiddle.add(panelSmooth);

     final JLabel surfaceTensionLabel = new JLabel();
     surfaceTensionLabel.setText("Surface Tension:  ");
     panelSmooth.add(surfaceTensionLabel);

     radioButton0 = new JRadioButton();
     radioButton0.setSelected(false);
     bGroup2.add(radioButton0);
     radioButton0.setText("0");
     panelSmooth.add(radioButton0);

     radioButton1 = new JRadioButton();
     radioButton1.setSelected(true);
     bGroup2.add(radioButton1);
     radioButton1.setText("1");
     panelSmooth.add(radioButton1);

     radioButton2 = new JRadioButton();
     bGroup2.add(radioButton2);
     radioButton2.setText("2");
     panelSmooth.add(radioButton2);

     radioButton3 = new JRadioButton();
     bGroup2.add(radioButton3);
     radioButton3.setText("3");
     panelSmooth.add(radioButton3);

     final JPanel panelSpin = new JPanel();
     panelMiddle.add(panelSpin);

     final JLabel rotationLabel = new JLabel();
     rotationLabel.setText("Rotation: ");
     panelSpin.add(rotationLabel);
     spinSlider = new JSlider(-100, 100, (int)(100*spinRate/0.2));
     panelSpin.add(spinSlider);
     
     JPanel panelBumps = new JPanel();
     panelBumps.setLayout(new BorderLayout());
     panelView.add(panelBumps);

     final JPanel panelBumpButtons = new JPanel();
     panelBumps.add(panelBumpButtons, BorderLayout.NORTH);

     final JLabel textureLabel = new JLabel();
     textureLabel.setText("Texture:");
     panelBumpButtons.add(textureLabel);
     
     trianglesRadioButton = new JRadioButton();
     trianglesRadioButton.setSelected(true);
     bGroup3.add(trianglesRadioButton);
     trianglesRadioButton.setText("Triangles");
     panelBumpButtons.add(trianglesRadioButton);

     assignedRadioButton = new JRadioButton();
     bGroup3.add(assignedRadioButton);
     assignedRadioButton.setText("Assigned");
     panelBumpButtons.add(assignedRadioButton);

     solidTextureRadioButton = new JRadioButton();
     bGroup3.add(solidTextureRadioButton);
     solidTextureRadioButton.setText("Solid");
     panelBumpButtons.add(solidTextureRadioButton);

     final JPanel panelCentering = new JPanel();
     panelCentering.setLayout(new FlowLayout());
     panelCentering.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
     panelBumps.add(panelCentering);

     final JPanel panelBumpSliders = new JPanel();
     panelCentering.add(panelBumpSliders);
     panelBumpSliders.setLayout(new GridLayout(0, 2));
     alphaSlider = new JSlider(-100, 100, (int)(100*alpha));
     panelBumpSliders.add(alphaSlider);
     alphaSlider.addChangeListener(new ChangeListener(){
        public void stateChanged(ChangeEvent e) { alphaStateChanged(e); }});
     alphaLabel = new JLabel("Height: " + alpha);
     panelBumpSliders.add(alphaLabel);
     betaSlider = new JSlider(1, 99, (int)(100*beta));
     panelBumpSliders.add(betaSlider);
     betaSlider.addChangeListener(new ChangeListener(){
        public void stateChanged(ChangeEvent e) { betaStateChanged(e); }});
     betaLabel = new JLabel("Width: " + beta);
     panelBumpSliders.add(betaLabel);
     gammaSlider = new JSlider(-100, 100, (int)(100*gamma));
     panelBumpSliders.add(gammaSlider);
     gammaSlider.addChangeListener(new ChangeListener(){
        public void stateChanged(ChangeEvent e) { gammaStateChanged(e); }});
     gammaLabel = new JLabel("Height: " + alpha);
     panelBumpSliders.add(gammaLabel);

     final JPanel panelBlendTexture = new JPanel();
     panelBumps.add(panelBlendTexture, BorderLayout.SOUTH);

     blendTextureCheckBox = new JCheckBox();
     blendTextureCheckBox.setText("Blend Texture");
     panelBlendTexture.add(blendTextureCheckBox);

     final JPanel panelExport = new JPanel();
     final GridBagLayout gridBagLayout_1 = new GridBagLayout();
     gridBagLayout_1.columnWidths = new int[] {0,7};
     panelExport.setLayout(gridBagLayout_1);
     panelView.add(panelExport);

     final JButton exportStlButton = new JButton();
     exportStlButton.addActionListener(new ActionListener() {
        public void actionPerformed(final ActionEvent arg0) { stlFlag=true;}
     });
     exportStlButton.setText("Export STL");
     final GridBagConstraints gridBagConstraints_8 = new GridBagConstraints();
     gridBagConstraints_8.gridx = 0;
     gridBagConstraints_8.gridy = 0;
     gridBagConstraints_8.insets = new Insets(5, 20, 10, 15);
     panelExport.add(exportStlButton, gridBagConstraints_8);
     
     frame.getRootPane().setDefaultButton(doButton);   //default button

     progressBar = new JProgressBar();
     panelExport.add(progressBar, new GridBagConstraints());

     canvas.addGLEventListener(new GLrenderer(this));
     contentPane.add(canvas, BorderLayout.CENTER);
     canvas.setPreferredSize(new Dimension(800,800));

     final JPanel bottomPanels = new JPanel();
     bottomPanels.setLayout(new BorderLayout());
     panelScript.add(bottomPanels, BorderLayout.SOUTH);

     final JPanel bottomPanel = new JPanel();
     bottomPanels.add(bottomPanel);
     
     startScriptButton = new JButton();
     startScriptButton.addActionListener(new ActionListener() {
        public void actionPerformed(final ActionEvent arg0) { startScript(); }
     });
     startScriptButton.setText("Start");
     bottomPanel.add(startScriptButton);

     nScript = new JTextField();
     nScript.setText("   1000   ");
     bottomPanel.add(nScript);

     continueButton = new JButton();
     continueButton.addActionListener(new ActionListener() {
        public void actionPerformed(final ActionEvent e) { continueScript(); }
     });
     continueButton.setText("Continue");
     bottomPanel.add(continueButton);

     stopButton = new JButton();
     stopButton.addActionListener(new ActionListener() {
        public void actionPerformed(final ActionEvent e) { stopScript(); }
     });
     stopButton.setText("Stop");
     bottomPanel.add(stopButton);
     
     startScriptButton.setEnabled(true);
     continueButton.setEnabled(false);
     stopButton.setEnabled(false);

     checkBoxVideo = new JCheckBox();
     checkBoxVideo.setToolTipText("Record frames for video");
     checkBoxVideo.setText("");
     bottomPanel.add(checkBoxVideo);

     final JPanel panelStatus = new JPanel();
     bottomPanels.add(panelStatus, BorderLayout.NORTH);

     scriptStatusLabel = new JLabel();
     scriptStatusLabel.setText("script status");
     panelStatus.add(scriptStatusLabel);
     
     stlFileChooser.setAcceptAllFileFilterUsed(false);
     stlFileChooser.addChoosableFileFilter(new STLFileFilter());
     gfsFileChooser.setAcceptAllFileFilterUsed(false);
     gfsFileChooser.addChoosableFileFilter(new GFSFileFilter());
     
     frame.pack();

     frame.setVisible(true);
     canvas.requestFocus();
      
   }


   ///////////////////////////////// GUI Interactions //////////////////////////
   
  //how many iterations of the smoothing algorithm
  public int nSmooth(){
     if (radioButton0.isSelected()) return 0;
     if (radioButton1.isSelected()) return 1;
     if (radioButton2.isSelected()) return 2;
     return 3;
  }
  
  public void report(String msg){
     JOptionPane.showMessageDialog(frame, msg, "Growth/Form warning", JOptionPane.WARNING_MESSAGE);
  }
  
  public void alphaStateChanged(ChangeEvent e) {
      alpha = alphaSlider.getValue()/100.0;
      alphaLabel.setText("Height: " + alpha);      
   }
   
   public void betaStateChanged(ChangeEvent e) {
      beta = betaSlider.getValue()/100.0;
      betaLabel.setText("Width: " + beta);      
   }
   
   public void gammaStateChanged(ChangeEvent e) {
      gamma = gammaSlider.getValue()/100.0;
      gammaLabel.setText("Shoulder: " + gamma);      
   }
   
   public float getSpinRate(){
      return (float) (spinSlider.getValue()*0.2/100);
   }
   
   //////////////////////// File IO stuff ////////////////////////////////////////

   //read in a new script
   void readScript(){
      stopScript();                   //stop previous one if it is running
      if (gfsFileChooser.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION ) return;
      String gfsFileName = gfsFileChooser.getSelectedFile().getPath();
      try{
         File gfFile = new File(gfsFileName);
         BufferedReader br = new BufferedReader(new FileReader(gfFile));
         StringBuffer txt = new StringBuffer();
         while (br.ready()) {txt.append(br.readLine()); txt.append("\n");}
         script.setText(txt.toString());
         br.close();
      }
      catch(Exception e){
         report("Error writing file " + gfsFileName);
         return;
      }
   }
   
   //write out the script
   void writeScript(){
      if (gfsFileChooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION ) return;
      String gfsFileName = gfsFileChooser.getSelectedFile().getPath();
      gfsFileName = forceExtension(gfsFileName, "gfs");     // annex .gf if user didn't
      try{
         File gfsFile = new File(gfsFileName);
         BufferedWriter bw = new BufferedWriter(new FileWriter(gfsFile));
         bw.write(script.getText());
         bw.close();
      }
      catch(Exception e){
         report("Error writing file " + gfsFileName);
         return;
      }
   }
   
   void exportBinarySTL(){              // TODO: use DataWriter instead
     if (!displayReady) return;
     if (stlFileChooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION ) return;
     String stlFileName = stlFileChooser.getSelectedFile().getPath();
     stlFileName = forceExtension(stlFileName, "stl");   // annex .stl if user didn't
     FileOutputStream stlStream;
     String headerText = "George W. Hart, www.georgehart.com";
     byte[] headerBytes = new byte[headerText.length()];
     for (int i=0; i<headerText.length(); i++)
       headerBytes[i] = (byte) headerText.charAt(i);

     try{
       File stlFile = new File(stlFileName);
       stlStream = new FileOutputStream(stlFile);
     }
     catch(FileNotFoundException e){
       report("Error opening file " + stlFileName);
       return;
     }
     try{
       frame.getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
       stlStream.write(headerBytes);                 // STL file header
       for (int i=headerText.length(); i<80; i++)
         stlStream.write((byte) 0);                  // pad to 80 chars
       DisplayInfo d = getDisplayInfo();
       int numTriangles = d.tri.length / 3;
       stlStream.write((numTriangles>> 0) & 255);       // send out int as 4 bytes
       stlStream.write((numTriangles>> 8) & 255);
       stlStream.write((numTriangles>>16) & 255);
       stlStream.write((numTriangles>>24) & 255);
       int i=0;
       while (i < d.tri.length) {
          float[] p1 = d.vert[d.tri[i++]]; 
          float[] p2 = d.vert[d.tri[i++]]; 
          float[] p3 = d.vert[d.tri[i++]]; 
          //XYZ normal = XYZ.unit(XYZ.cross(XYZ.minus(t.v2.xyz, t.v1.xyz), XYZ.minus(t.v3.xyz, t.v1.xyz)));
          XYZ normal = new XYZ(0,0,0);                //this is OK for most 
         binaryWrite((float) -normal.x, stlStream);
         binaryWrite((float) normal.y, stlStream);
         binaryWrite((float) normal.z, stlStream);
         binaryWrite((float) -p1[0], stlStream);
         binaryWrite((float) p1[1], stlStream);
         binaryWrite((float) p1[2], stlStream);
         binaryWrite((float) -p3[0], stlStream); // swapped
         binaryWrite((float) p3[1], stlStream);
         binaryWrite((float) p3[2], stlStream);
         binaryWrite((float) -p2[0], stlStream);
         binaryWrite((float) p2[1], stlStream);
         binaryWrite((float) p2[2], stlStream);
         stlStream.write((byte) 0);               // pad to 50 chars
         stlStream.write((byte) 0);               // pad to 50 chars
         if (i%300 == 0) {            //No show, since in GUI thread
            progressBar.setValue((int)(100.0*((double)i/(double)d.tri.length)));
         }
       }
       stlStream.close();
         }
     catch(IOException ex){
       report("Error writing STL file " + stlFileName);
       return;
     }
     finally{
       progressBar.setValue(0);
       frame.getContentPane().setCursor(null);
       //Toolkit.getDefaultToolkit().beep();
     }
   }

   // send out float as 4 bytes for STL binary format
   void binaryWrite (float x, FileOutputStream stream) throws IOException{
     int fourBytes = Float.floatToIntBits(x);
     stream.write((fourBytes>> 0) & 255);
     stream.write((fourBytes>> 8) & 255);
     stream.write((fourBytes>>16) & 255);
     stream.write((fourBytes>>24) & 255);
   }

   // annex given lower-case 3-letter extension to file name if user didn't
   String forceExtension(String name, String ext){
     String end = ("xxxx" + name).substring(name.length());
     if (end.toLowerCase().equals("." + ext)) return name;
     else return name + "." + ext;
   }
   
   
   ///////////////////////////display capture for video/////////////////////////
   
   public void capture(String filename, int i){
      try{
         String index = Integer.toString(i);   //make 5-digit index, e.g., 00001
         index = "00000".substring(index.length()) + index;
         Rectangle rectangle = new Rectangle(canvas.getLocationOnScreen(), canvas.getSize());
         BufferedImage image = robot.createScreenCapture(rectangle);
         File file = new File(filename + index + ".png"); //Save the screenshot as a png
         ImageIO.write(image, "png", file);
      }
      catch (Exception e) { System.out.println(e.getMessage()); }
   }
}


