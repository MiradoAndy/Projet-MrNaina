package src;

import java.io.IOException;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

public class PrintUrlServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String url = request.getRequestURL().toString();
        response.getWriter().println("URL: " + url);
    }

}