package hello;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import com.slack.api.model.block.LayoutBlock;

public class Cache {
    private static final Cache INSTANCE = new Cache();

    private Hashtable<String, List<LayoutBlock>> viewHT = new Hashtable<String, List<LayoutBlock>>();
    private Hashtable<String, LocalDateTime> timeHT = new Hashtable<String, LocalDateTime>();
    private Hashtable<String, String> stringHT = new Hashtable<String, String>();


    private LocalDateTime theTime = LocalDateTime.now();
    private int cacheExpiryInHour = Integer.parseInt(System.getenv("CACHE_VIEW"));

    private String delimiter = "__";
    private final String JenkinsPass = "JENKINS_PASS";
    private final String JenkinsUser = "JENKINS_USERS";
    private final String cemApiKey = "CEM_APIKEY";

    private Cache() {
    }

    public static Cache getInstance() {
        return INSTANCE;
    }

    public String getJenkinsPassword() {
        return stringHT.get(this.JenkinsPass);
    }
    public void setJenkinsPassword(String thePassword) {
        stringHT.put(this.JenkinsPass, thePassword);
    }

    public String getJenkinsUsername() {
        return stringHT.get(this.JenkinsUser);
    }
    public void setJenkinsUsername(String theUser) {
        stringHT.put(this.JenkinsUser, theUser);
    }

    public String getCemApikey() {
        return stringHT.get(this.cemApiKey);
    }
    public void setCemApikey(String theKey) {
        stringHT.put(this.cemApiKey, theKey);
    }


    public boolean isViewCached(String selectString, String checkboxString) {
        String theKey = getKey(selectString, checkboxString);
        boolean result = false;

        if (viewHT.containsKey(theKey) && timeHT.containsKey(theKey)) {
            if (LocalDateTime.now().compareTo(timeHT.get(theKey).plus(cacheExpiryInHour, ChronoUnit.HOURS)) < 0) {
                result = true;
            }
        }
        return result;
    }

    public List<LayoutBlock> getView(String selectString, String checkboxString) {
        List<LayoutBlock> result = new ArrayList<LayoutBlock>();

        String theKey = getKey(selectString, checkboxString);
        if (viewHT.containsKey(theKey) && timeHT.containsKey(theKey)) {
            if (LocalDateTime.now().compareTo(timeHT.get(theKey).plus(cacheExpiryInHour, ChronoUnit.HOURS)) < 0) {
                result = viewHT.get(theKey);
            }
        }
        return result;
    }

    public void updateView(String selectString, String checkboxString, List<LayoutBlock> theView) {
        String theKey = getKey(selectString, checkboxString);
        viewHT.put(theKey, theView);
        timeHT.put(theKey, LocalDateTime.now());
    }

    private String getKey(String selectString, String checkboxString) {
        return selectString + this.delimiter + checkboxString;
    }

    public LocalDateTime getTime() {
        return theTime;
    }

    public String getInfo() {
        return "size: " + viewHT.size() + ", keys: " + Arrays.toString(viewHT.keySet().toArray());
    }
}