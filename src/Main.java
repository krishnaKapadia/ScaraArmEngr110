import ecs100.*;
import java.util.*;
import java.io.*;
import java.awt.*;


/** <description of class Main>
 */
public class Main{

    private Arm arm;
    private Drawing drawing;
    private ToolPath tool_path;
    
    private String fName = "";
    // state of the GUI
    private int state; // 0 - nothing
                       // 1 - inverse point kinematics - point
                       // 2 - enter path. Each click adds point  
                       // 3 - enter path pause. Click does not add the point to the path
    
    /**      */
    public Main(){
        UI.initialise();
        UI.addButton("xy to angles", this::inverse);
        UI.addButton("Enter path XY", this::enter_path_xy);
        UI.addButton("Save path XY", this::save_xy);
        UI.addButton("Load path XY", this::load_xy);
        UI.addButton("Save path Ang", this::save_ang);
        UI.addButton("Load path Ang:Play", this::load_ang);
        UI.addButton("Send to PI", this::sendToPi);
        UI.addButton("Draw Circle", this::drawCircle);
        UI.addButton("Get Image", this::showImage);
        UI.addButton("Quit", UI::quit);
        UI.setMouseMotionListener(this::doMouse);
        UI.setKeyListener(this::doKeys);

        //ServerSocket serverSocket = new ServerSocket(22);
        this.arm = new Arm();
        this.drawing = new Drawing();
        this.tool_path = new ToolPath();
        this.run();
        arm.draw();
    }
    
    public void drawCircle(){
        int radius = 50;
        double centerX = 334;
        double centerY = 122;
        
        for(int i = 0; i <= 380; i += 14){
            double X = centerX + radius * Math.cos((((double) i/180) * Math.PI));
            double Y = centerY + radius * Math.sin((((double) i/180) * Math.PI));
            drawing.add_point_to_path(X, Y, true);
        }
        
    }
    
    public void showImage(){
        fName = UIFileChooser.save();
    }

    public void sendToPi() {

        try {
            ProcessBuilder pb = new ProcessBuilder("script", "test", "scp line.txt pi@10.140.66.166:/home/pi/Arm/");
            Process p = pb.start();
            InputStream stream = p.getInputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
            Scanner s = new Scanner(stream);
            while (p.isAlive()) {
                String str = s.next();
                UI.println(str);

                if (str.contains("password"))
                    writer.write("pi\n");
                writer.flush();
            }

        }catch(Exception e){
            UI.println(e);
        }
    }
    
    public void doKeys(String action){
        UI.printf("Key :%s \n", action);
        if (action.equals("b")) {
            // break - stop entering the lines
            state = 3;
        }
               
    }
    
    
    public void doMouse(String action, double x, double y) {
         //UI.printf("Mouse Click:%s, state:%d  x:%3.1f  y:%3.1f\n",
         //   action,state,x,y);
        UI.clearGraphics();
        if(fName != "") UI.drawImage(fName, 164, 59);
        String out_str=String.format("%3.1f %3.1f",x,y);
        UI.drawString(out_str, x+10,y+10);
         // 
         if ((state == 1)&&(action.equals("clicked"))){
          // draw as 
          arm.inverseKinematic(x,y);
          arm.draw();
          return;
        }
        
         if ( ((state == 2)||(state == 3))&&action.equals("moved") ){
          // draw arm and path
          arm.inverseKinematic(x,y);
          arm.draw();
         
          // draw segment from last entered point to current mouse position
          if ((state == 2)&&(drawing.get_path_size()>0)){
            PointXY lp = new PointXY();
            lp = drawing.get_path_last_point();
            //if (lp.get_pen()){
               UI.setColor(Color.GRAY);
               UI.drawLine(lp.get_x(),lp.get_y(),x,y);
           // }
          }
           drawing.draw();
        }
        
        // add point
        if ((state == 2) &&(action.equals("clicked"))){
            // add point(pen down) and draw
            UI.printf("Adding point x=%f y=%f\n",x,y);
            drawing.add_point_to_path(x,y,true); // add point with pen down
            
            arm.inverseKinematic(x,y);
            arm.draw();
            drawing.draw();
            drawing.print_path();
        }
        
        
        if ((state == 3) &&(action.equals("clicked"))){
            // add point and draw
            //UI.printf("Adding point x=%f y=%f\n",x,y);
            drawing.add_point_to_path(x,y,false); // add point wit pen up
            
            arm.inverseKinematic(x,y);
            arm.draw();
            drawing.draw();
            drawing.print_path();
            state = 2;
        }
        
    }
   
    
    public void save_xy(){
        state = 0;
        String fname = UIFileChooser.save();
        drawing.save_path(fname);
    }
    
    public void enter_path_xy(){
         state = 2;
    }
    
    public void inverse(){
         state = 1;
         arm.draw();
    }
    
    public void load_xy(){
        state = 0;
        String fname = UIFileChooser.open();
        drawing.load_path(fname);
        drawing.draw();
        
        arm.draw();
    }
    
    // save angles into the file
    public void save_ang(){
        state = 0;
        String fname = UIFileChooser.save();
        tool_path.convert_drawing_to_angles(drawing, arm, fname);
        tool_path.convert_angles_to_pwm(arm, fname);
        tool_path.save_pwm_file(fname);
    }
    
    
    public void load_ang(){
        
    }
    
    public void run() {
        while(true) {
            arm.draw();
            UI.sleep(20);
        }
    }

    public static void main(String[] args){
        Main obj = new Main();
    }    

}
