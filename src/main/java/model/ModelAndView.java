package model;

import java.util.HashMap;

public class ModelAndView {
    private String urlSuivant;
    private HashMap<String,String> list ;

    public String getUrlSuivant() {
        return urlSuivant;
    }
    public void setUrlSuivant(String urlSuivant) {
        this.urlSuivant = urlSuivant;
    }
    public HashMap<String, String> getList() {
        return list;
    }
    public void setList(HashMap<String, String> list) {
        this.list = list;
    }  
}

