package org.geoserver.gsmonitor_web;

import org.geoserver.web.GeoServerBasePage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.extensions.markup.html.form.select.Select;
import org.apache.wicket.extensions.markup.html.form.select.SelectOption;

public class Gsmonitor_web extends GeoServerBasePage {

	public Gsmonitor_web() {
		
		
		
		add(new Label("hellolabel", "Hello World!"));

	}

}
