package model;

public class UrlInfo {
    private String url;
    private HTTPmethode action;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public HTTPmethode getAction() {
        return action;
    }

    public void setAction(HTTPmethode action) {
        this.action = action;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true; // même objet
        if (o == null || getClass() != o.getClass()) // null ou pas un UrlInfo
            return false;
        UrlInfo other = (UrlInfo) o; // cast
        return url.equals(other.url) && action == other.action; // même url ET même méthode HTTP
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(url, action); // mêmes champs que equals
    }
}
