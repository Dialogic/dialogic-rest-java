/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package xmsMultiConfDemoAsync;


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
        XMSMultiConfDemoAsync myDemo = new XMSMultiConfDemoAsync();
        
        //Initialize with tne number of participants
        myDemo.Initialize(4);
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
