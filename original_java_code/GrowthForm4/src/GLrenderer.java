
import javax.media.opengl.GL;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.glu.GLU;
import javax.swing.event.MouseInputListener;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.List;

public class GLrenderer implements GLEventListener, KeyListener, MouseInputListener {
   private GrowthForm GF;
   private int mouseStartX;         //mouse control virtual trackball
   private int mouseStartY;
   private XYZ rotationAxis = new XYZ(0,1,0);
   private double rotationAngle = 0;   
   private double cameraDist = 20.;
   private static final GLU glu = new GLU();

   public GLrenderer(GrowthForm growthForm) {
      this.GF = growthForm;          // remember our creator
   }

   public void display(GLAutoDrawable gLDrawable) {
      final GL gl = gLDrawable.getGL();
      
//Experimented with materials and lighting, but didn't get adequate results, so commented:
//      float[] lightPos  = {-1.0f, 1.0f, 1.0f, 0.0f};    // light from upper left shoulder
//      float[] lightComp = {1.0f, 1.0f, 1.0f, 1.0f};
//      gl.glEnable(gl.GL_LIGHTING);
//      gl.glEnable(gl.GL_LIGHT0);
//      gl.glLightfv(gl.GL_LIGHT0, gl.GL_DIFFUSE, lightComp, 0);  //I added 0's here
//      gl.glLightfv(gl.GL_LIGHT0, gl.GL_SPECULAR, lightComp, 0); //I added 0's here
//      gl.glLightfv(gl.GL_LIGHT0, gl.GL_AMBIENT, lightComp, 0); //I added 0's here
//      gl.glLightModeli(gl.GL_LIGHT_MODEL_TWO_SIDE, 1);            // TODO
//      gl.glLightModeli(gl.GL_LIGHT_MODEL_LOCAL_VIEWER, 1);        // TODO

      gl.glClear(GL.GL_COLOR_BUFFER_BIT);
      gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
      gl.glClearColor(0.0f, 0.0f, 0.4f,1.0f);     // dark blue background
      gl.glLoadIdentity();
      gl.glTranslatef(0.0f, 0.0f, (float) -cameraDist);

      //gl.glRotatef(rotateT, 1.0f, 0.0f, 0.0f);
      gl.glRotatef(GF.getSpinAngle(), 0.0f, 1.0f, 0.0f);     //about Y axis
      //gl.glRotatef(rotateT, 0.0f, 0.0f, 1.0f);
      //gl.glRotatef(rotateT, 0.0f, 1.0f, 0.0f);
      gl.glRotatef((float) rotationAngle, (float) rotationAxis.x, (float) rotationAxis.y, (float) rotationAxis.z);
      
//      gl.glLightfv(gl.GL_LIGHT0, gl.GL_POSITION, lightPos, 0);   // position light
      
//      float[] mSpecular = Color.BLUE.getComponents(null);           // use RGB for specular
//      float[] mAmbient = {1.0f, 1.0f, 1.0f, 1.0f};
//      for (int i=0; i<3; i++) mAmbient[i]=0.25f*mSpecular[i];   // 0.25 RGB for ambient
//      float[] mDiffuse = {1.0f, 1.0f, 1.0f, 1.0f};
//      for (int i=0; i<3; i++) mDiffuse[i]=0.2f + 0.8f*mSpecular[i];  // brighten for diffuse
//      float mShininess = 90.0f;
//      gl.glMaterialfv(gl.GL_FRONT_AND_BACK, gl.GL_AMBIENT, mAmbient, 0);   //I added 0's here
//      gl.glMaterialfv(gl.GL_FRONT_AND_BACK, gl.GL_DIFFUSE, mDiffuse, 0);   //I added 0's here
//      gl.glMaterialfv(gl.GL_FRONT_AND_BACK, gl.GL_SPECULAR, mSpecular, 0);   //I added 0's here
//      gl.glMaterialf (gl.GL_FRONT_AND_BACK, gl.GL_SHININESS, mShininess);
      
 //     synchronized(GF.form){
        DisplayInfo d = GF.getDisplayInfo();         // d has display data: vert, col, tri, line
        if (d==null) return;
        
         gl.glBegin(GL.GL_TRIANGLES);
//         for (Triangle t : displayInfo.tris){
//            gl.glColor3f(t.v1.colorRed, t.v1.colorGreen, t.v1.colorBlue); 
//            gl.glVertex3f((float)t.v1.xyz.x, (float)t.v1.xyz.y, (float)t.v1.xyz.z);
//            gl.glColor3f(t.v2.colorRed, t.v2.colorGreen, t.v2.colorBlue); 
//            gl.glVertex3f((float)t.v2.xyz.x, (float)t.v2.xyz.y, (float)t.v2.xyz.z);
//            gl.glColor3f(t.v3.colorRed, t.v3.colorGreen, t.v3.colorBlue); 
//            gl.glVertex3f((float)t.v3.xyz.x, (float)t.v3.xyz.y, (float)t.v3.xyz.z);
//         }

         for (int i = 0; i<d.tri.length; i++){
          gl.glColor3f(d.col[d.tri[i]][0], d.col[d.tri[i]][1], d.col[d.tri[i]][2]); 
          gl.glVertex3f(d.vert[d.tri[i]][0], d.vert[d.tri[i]][1], d.vert[d.tri[i]][2]);
         }
         gl.glEnd();

         gl.glDisable(GL.GL_LIGHTING);            // Disable lights for edges and points
         gl.glColor3f(0.0f, 0.0f, 0.0f);                     //black edges
         gl.glBegin(GL.GL_LINES);                            //Start drawing edges (each twice if in 2 tris)
         for (int i = 0; i<d.line.length; i++){
            gl.glVertex3f(d.vert[d.line[i]][0], d.vert[d.line[i]][1], d.vert[d.line[i]][2]);
           }
//         Iterator<Cell> it = displayInfo.lines.iterator();
//         while (it.hasNext()){
//            Cell v = it.next();
//            gl.glVertex3f((float)v.xyz.x, (float)v.xyz.y, (float)v.xyz.z);   // send GL the endpoints
//         }
//         for (Cell v1 : GF.form.vertex) 
//            for (Cell v2 : v1.adj)
//               if(v1.index < v2.index){
//                  gl.glVertex3f((float)v1.xyz.x, (float)v1.xyz.y, (float)v1.xyz.z);   // send GL the endpoints
//                  gl.glVertex3f((float)v2.xyz.x, (float)v2.xyz.y, (float)v2.xyz.z);
//               }
         gl.glEnd();                                        // Done drawing edges
    //  }

      //spinAngle += 0.5f;  //moved upstairs for uniform spin with growth
   }

   public void displayChanged(GLAutoDrawable gL, boolean mode, boolean dev) {}

   public void init(GLAutoDrawable gLDrawable) {
      final GL gl = gLDrawable.getGL();
      gl.glShadeModel(GL.GL_SMOOTH);
      gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
      gl.glClearDepth(1.0f);
      gl.glEnable(GL.GL_DEPTH_TEST);
      gl.glDepthFunc(GL.GL_LEQUAL);
      gl.glHint(GL.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);
      gLDrawable.addKeyListener(this);
      gLDrawable.addMouseMotionListener(this);
      gLDrawable.addMouseListener(this);
   }

   public void reshape(GLAutoDrawable gLDrawable, int x, 
         int y, int width, int height) {
      final GL gl = gLDrawable.getGL();
      if (height <= 0) height = 1;
      final float h = (float)width / (float)height;
      gl.glMatrixMode(GL.GL_PROJECTION);
      gl.glLoadIdentity();
      glu.gluPerspective(50.0f, h, 1.0, 1000.0);
      gl.glMatrixMode(GL.GL_MODELVIEW);
      gl.glLoadIdentity();
   }

   public void keyPressed(KeyEvent e) {
      if(e.getKeyCode() == KeyEvent.VK_ESCAPE) {
         GrowthForm.displayT = null;
         System.exit(0);
      }
   }

   public void keyReleased(KeyEvent e) {}

   public void keyTyped(KeyEvent e) {}

   public void mouseClicked(MouseEvent me) {}

   public void mouseEntered(MouseEvent me) {}

   public void mouseExited(MouseEvent me) {}

   public void mousePressed(MouseEvent me) {
      mouseStartX=me.getX();
      mouseStartY=me.getY();
   }

   public void mouseReleased(MouseEvent me) {}

   public void mouseDragged(MouseEvent me) {
      int mouseX=me.getX();
      int mouseY=me.getY();
      if ((me.getModifiersEx()&me.BUTTON1_DOWN_MASK) != 0){
         XYZ mouseMove = new XYZ(mouseY-mouseStartY, mouseX-mouseStartX, 0);  //90 degr
         rotationAngle = XYZ.mag(mouseMove);
         if (rotationAngle < 1.0) rotationAxis = new XYZ(1,0,0);
         else rotationAxis = XYZ.unit(mouseMove);
      }
      else {
         cameraDist += (mouseY-mouseStartY)/50.;
         if (cameraDist < 2.0) cameraDist = 2.0;
      }
   }

   public void mouseMoved(MouseEvent me) {}
   
}
