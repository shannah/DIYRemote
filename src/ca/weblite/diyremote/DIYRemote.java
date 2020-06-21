package ca.weblite.diyremote;


import ca.weblite.irblaster.IRBlaster;
import com.codename1.components.ToastBar;
import com.codename1.io.JSONParser;
import static com.codename1.ui.CN.*;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Dialog;
import com.codename1.ui.Label;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import com.codename1.io.Log;
import com.codename1.ui.Toolbar;
import java.io.IOException;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.io.NetworkEvent;
import com.codename1.io.Preferences;
import com.codename1.io.Util;
import com.codename1.processing.Result;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.Button;
import static com.codename1.ui.ComponentSelector.$;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Sheet;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.layouts.LayeredLayout;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * This file was generated by <a href="https://www.codenameone.com/">Codename One</a> for the purpose 
 * of building native mobile applications using Java.
 */
public class DIYRemote {

    private Form current;
    private Resources theme;
    private BrowserComponent browser;

    public void init(Object context) {
        // use two network threads instead of one
        updateNetworkThreadCount(2);

        theme = UIManager.initFirstTheme("/theme");

        // Enable Toolbar on all Forms by default
        Toolbar.setGlobalToolbar(true);

        // Pro only feature
        Log.bindCrashProtection(true);

        addNetworkErrorListener(err -> {
            // prevent the event from propagating
            err.consume();
            if(err.getError() != null) {
                Log.e(err.getError());
            }
            Log.sendLogAsync();
            Dialog.show("Connection Error", "There was a networking error in the connection to " + err.getConnectionRequest().getUrl(), "OK", null);
        });        
    }
    
    private int[] toIntArray(List<Number> l) {
        int len = l.size();
        int[] out = new int[len];
        for (int i=0; i<len; i++) {
            out[i] = l.get(i).intValue();
        }
        return out;
    }
    
    public void start() {
        if(current != null){
            current.show();
            return;
        }
        Form hi = new Form("Hi World", new LayeredLayout());
        hi.getToolbar().hideToolbar();
        browser = new BrowserComponent();
        String url = Preferences.get("remote-url", null);
        if (url == null) {
            url = "https://weblite.ca/remote.html";
            Preferences.set("remote-url", url);
        }
        browser.setURL(url);
        browser.setDebugMode(true);
        //JSONParser parser = new JSONParser();
        browser.addWebEventListener(BrowserComponent.onLoad, evt->{
            
            System.out.println("Adding JS Callback");
            browser.addJSCallback("window.transmit=function(code){"
                    + "callback.onSuccess(code);"
                    + "};window.sendPronto=window.transmit;", jsRes->{
                        System.out.println("In JS callback");
               //JSONParser p = (JSONParser)parser;
               
               BrowserComponent.JSRef ref = jsRes;
               try {
                  //Map map = p.parseJSON(new StringReader(ref.getValue()));
                  String code = ref.getValue();
                  IRCommand command = new IRCommand(code);
                  System.out.println("Freq: "+command.getFrequency()+", code: "+Arrays.toString(command.getPattern()));
                  if (!IRBlaster.hasIrEmitter()) {
                      ToastBar.showErrorMessage("This device is not equipped with an Infrared Blaster");
                      return;
                  }
                  
                 IRBlaster.transmit(command.getFrequency(), command.getPattern());
               } catch (Throwable t) {
                   Log.e(t);
               }
            });
        });
        
        hi.add(browser);
        
        Button settings = new Button();
        settings.setMaterialIcon(FontImage.MATERIAL_SETTINGS);
        
        
            
        
        settings.addActionListener(evt->{
            TextField urlField = new TextField();
            $(urlField).selectAllStyles().setFontSizeMillimeters(2.5f);
            urlField.setText(browser.getURL());
            Button go = new Button("Go");
            
            
            Button findCodes = new Button("Find Codes");
            findCodes.setMaterialIcon(FontImage.MATERIAL_SEARCH);
            
            
            Button help = new Button("Help");
            help.setMaterialIcon(FontImage.MATERIAL_HELP);
            
            $(findCodes, help).selectAllStyles().setFontSizeMillimeters(2.5f);
            
            Button back = new Button();
            back.setMaterialIcon(FontImage.MATERIAL_ARROW_BACK);
            back.addActionListener(evt2->{
                browser.back();
            });

            Button forward = new Button();
            forward.setMaterialIcon(FontImage.MATERIAL_ARROW_FORWARD);
            forward.addActionListener(evt2->browser.forward());

            Container controlsCnt = new Container(BoxLayout.x());
            controlsCnt.addAll(back, forward);
            
            
            Container cnt = BorderLayout.centerEastWest(urlField, go, null);
            cnt.add(BorderLayout.SOUTH, GridLayout.encloseIn(2, findCodes, help));
            cnt.add(BorderLayout.NORTH, BorderLayout.east(controlsCnt));
            Sheet sheet = new Sheet(null, "Change Page");
            sheet.setLayout(BoxLayout.y());
            sheet.add(cnt);
            sheet.setPosition(BorderLayout.NORTH);
            
            go.addActionListener(evt2->{
                browser.setURL(urlField.getText());
                Preferences.set("remote-url", urlField.getText());
                sheet.back();
            });
            
            help.addActionListener(evt2->{
                browser.setURL("https://shannah.github.io/DIYRemote");
                sheet.back();
            });
            
            findCodes.addActionListener(evt2->{
                browser.setURL("http://irdb.tk/find/");
                sheet.back();
            });
            
           
            sheet.show();
            
            
        });
        
        hi.add(settings);
        
        
        
        LayeredLayout ll = (LayeredLayout)hi.getLayout();
        ll.setInsets(settings, "auto auto 0 0");
        
        
        
        
        hi.show();
    }

    public void stop() {
        current = getCurrentForm();
        if(current instanceof Dialog) {
            ((Dialog)current).dispose();
            current = getCurrentForm();
        }
    }
    
    public void destroy() {
    }
    // From https://rileymacdonald.ca/2017/10/14/tutorial-how-to-write-an-android-app-widget-to-control-your-television/
    public class IRCommand {
        private int frequency;
        private int[] pattern;

        IRCommand(final String irData) {
            List<String> list = new ArrayList<>(Arrays.asList(Util.split(irData, " ")));
            list.remove(0);
            int frequency = Integer.parseInt(list.remove(0), 16); // frequency
            list.remove(0);
            list.remove(0);

            frequency = (int) (1000000 / (frequency * 0.241246));
            int pulses = 1000000 / frequency;
            int count;

            int[] pattern = new int[list.size()];
            for (int i = 0; i < list.size(); i++) {
                count = Integer.parseInt(list.get(i), 16);
                pattern[i] = count * pulses;
            }

            this.frequency = frequency;
            this.pattern = pattern;
        }

        int getFrequency() { return frequency; }
        int[] getPattern() { return pattern; }
    }
    
}
