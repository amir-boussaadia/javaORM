package org.esgi.web;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.velocity.app.Velocity;
import org.esgi.module.index.Index;
import org.esgi.module.user.Connect;
import org.esgi.module.user.New;
import org.esgi.module.user.NewResult;
import org.esgi.module.user.NewSupport;
import org.esgi.module.user.Notification;
import org.esgi.module.user.Resultat;
import org.esgi.module.user.Support;
import org.esgi.orm.model.User;
import org.esgi.web.action.IAction;
import org.esgi.web.action.IContext;
import org.esgi.web.layout.LayoutRenderer;
import org.esgi.web.route.Router;

/**
 * Le frontcontroller est
 * le lien entre tomcat et votre
 * framework. Il génère le context pour 
 * chaque requete et peu contenir des filtres 
 * d'entree et de sortie pour chaque requette
 * exemple validateur de champs ou compression gzip.
 *
 */
public class FrontController extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	
	public static final String NAMESESSIONUSER = "user_co";
	
	Router router = new Router();
	Properties properties = new Properties();
	private LayoutRenderer layoutRender;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		
		String configFile = config.getServletContext().getInitParameter("config");
		String path = config.getServletContext().getRealPath("/");

		try {
			properties.load(new FileInputStream(path +"/" + configFile));
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Should be in init.

		Properties configVelocity = new Properties();
		configVelocity.setProperty("file.resource.loader.path", config.getServletContext().getRealPath("/") + properties.getProperty("template.path")+ "/");
		Velocity.init(configVelocity);
		
		registerAction(new NewSupport());
		registerAction(new Support());
		registerAction(new NewResult());
		registerAction(new Resultat());
		registerAction(new New());
		registerAction(new Notification());
		registerAction(new Connect());
		registerAction(new Index());
		
		layoutRender = new LayoutRenderer();
		
	} 
	
	@Override
	public void service(HttpServletRequest 
			request, HttpServletResponse response)
					throws ServletException, IOException {
		String url = request.getPathInfo();
		IContext context = createContext(request, response);
		IAction action = router.find(url, context);
		
		context.setPageTitle("SCHOOL MANAGER");
		
		User u = null;
		HttpSession session = context.getRequest().getSession(false);
		if (session == null) {
		    // Not created yet. 
		    session = context.getRequest().getSession();
		} else {
		    // Already created
			u = (User)session.getAttribute(FrontController.NAMESESSIONUSER);
		}
		
		((Context)context).session = session;
		
		// LOAD USER IN CONTEXT
		((Context)context).getVelocityContext().put("user", u);
		
		properties.put("context", request.getContextPath());

		if (null != action){

			if (null == action.getLayout()) {
				try {
					action.execute(context);
				} catch (Exception e) {
					throw new ServletException(e);
				}
			} else {
				try {
					layoutRender.render(action, context, router);
				} catch (Exception e) {
					throw new ServletException(e);
				}
			}
		}

	}

	private IContext createContext(HttpServletRequest request, 
			HttpServletResponse response) {
		return new Context(request, response, properties);
	}

	public void registerAction(IAction action) {
		router.register(action);
	}
}
