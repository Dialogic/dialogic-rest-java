/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package xmsConfDemoAsync;


/**
 *
 * @author dwolansk
 */
public class XMSConfDemoAsyncTest  {

    
    /**
     * @param args the command line arguments
     */
        
    public static void main(String[] args) {
        // TODO code application logic here
        XMSConfDemoAsync myDemo = new XMSConfDemoAsync();
        
        myDemo.RunDemo();
        
        //At this point event handler thread will process all events so this thread just waits to complete
        while(myDemo.isRunning)   {
            Sleep(1000);
        }
        
    }
      public static void Sleep(int time){
            try {
                Thread.sleep(time);
            } catch (InterruptedException ex) {
                System.out.print(ex);
            }
                   
        }
}
