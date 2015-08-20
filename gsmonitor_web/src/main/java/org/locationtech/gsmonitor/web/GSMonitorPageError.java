package org.locationtech.gsmonitor.web;

import org.apache.wicket.markup.html.basic.Label;
import org.geoserver.web.GeoServerSecuredPage;

public class GSMonitorPageError extends GeoServerSecuredPage {
	
	
	public GSMonitorPageError() {
		add(new Label("errorMessage", "Monitor cannot be configured due to error. Please consult log files for details."));
	}

}
