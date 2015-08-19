# GS Feature Monitor

GS Feature Monitor is open source software initially developed as part of GSoC (Google Summer of Code) under the mentorship of [LocationTech](http://www.locationtech.org).
The tool is extension to the [GeoSever Monitor](http://docs.geoserver.org/latest/en/user/extensions/monitoring/index.html).
The tool captures WMS Map Request meta-data, converts them to OpenGIS Simple Features and stores in them a vector data store.

The intent of the effort is to provide a map request data capture tool in support analytics for map requests over time. 
One such use case is identifying trends of interest in specific geographic areas.  For example, map requests for a particular city may increase during a period of special events such as World Cup games, as visitors seek directions, places to eat, etc.

The Geometry capture for each map request is the bounding box converted into a square polygon in the 'envelope' attribute of the simple feature.  In absence of bounding box, the envelope assumes the world using default CRS EPSG:4236.

The monitor, oddly enough, sends many requests with an 'id' of -1.  These are set to the current time in milliseconds from the epoch (January 1st, 1970).  

Since some data stores have issues with String attributes exceeding 255 characters, all string attributes are clipped at 255 characters.

## Installing the Monitor

(1) Follow GeoServer's documention to install the Monitor Extension.

(2) If not already created, create 'monitoring' directory in GeoServer's configured data directory (e.g. data_dir).

(3) Adjust or add a monitor.properties file to the monitoring directory, setting the property 'storage' to 'vector'
```
storage=vector
```

(4) Add a new file featureMonitor.properties to the monitoring directory, containing all required properties of a writable vector datastore configured within GeoServer.
The following example is for H2:
```
database=E:\geoserver-2.7.0-bin\geoserver-2.7.0\db
dbtype=h2
```

(5) Build the tool
```
% mvn clean install
```

(6) Copy the target jar file from 'gsmonitor/target' directory to the GeoServer's web application 'lib' directory.
``` 
% cp .\gsmonitor\target\gsmonitor-0.0.1-SNAPSHOT.jar  E:\geoserver-2.7.0-bin\webapps\geoserver\WEB-INF\lib
```

The default name of the feature type describing a WMS request is 'requestDataFeature'.  The default can be changed by editing the featureMonitor.properties, as described above, setting the property 'mds.type' to the type name.
```
mds.type=wmsMetaData
```
## Configuring the Monitor using the UI

Accompany the monitor is an additional tool to configure the monitor within GeoServer.  This section provides installation and examples of use.

### Installing the Monitor UI Tool
Copy the target jar file from gsmonitor_web/target directory to the GeoServer's web application 'lib' directory.
``` 
% cp .\gsmonitor_web\target\gsmonitor_web-0.0.1-SNAPSHOT.jar  E:\geoserver-2.7.0-bin\webapps\geoserver\WEB-INF\lib
```
### Using the Tool
