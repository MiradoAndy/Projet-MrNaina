package mg.itu.controllers;

import mg.itu.annotation.MonController;
import mg.itu.annotation.MaFonction;

@MonController(url = "/test1")
public class Test1Controller {

    @MaFonction
    public void afficherAccueil() {}

    @MaFonction
    public void listerElements() {}

    public void methodeNonExposee() {}
}
