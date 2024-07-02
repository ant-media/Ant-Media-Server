package io.antmedia.rest.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class UserScope implements Serializable {

    public HashMap<String, UserType> appNameUserTypeMap;

    public boolean systemRights = false;


    public UserScope(){

    }

    public HashMap<String, UserType> getAppNameUserTypeMap() {
        return appNameUserTypeMap;
    }

    public boolean isSystemRights() {
        return systemRights;
    }

    public void setSystemRights(boolean systemRights) {
        this.systemRights = systemRights;
    }


}
