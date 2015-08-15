# GS Feature Monitor

GS Feature Monitor is open source software initially developed as part of GSoC (Google Summer of Code) under the mentorship of [LocationTech](www.locationtech.org).
The tool is extension to the [GeoSever Monitor](http://docs.geoserver.org/latest/en/user/extensions/monitoring/index.html).
The tool captures WMS Map Request meta-data, converts them to OpenGIS Simple Features and stores in them a vector data store.

The intent of the effort is to provide a map request data capture tool in support analytics for map requests over time. 
One such use case is identifying trends of interest in specific geographic areas.  For example, map requests for a particular city
may increase during a period of special events such as World Cup games, as visitors seek directions, places to eat, etc.

## Installing the Monitor

1. Follow GeoServer's documention to install the Monitor Extension.
2. If not already created, create 'monitoring' directory in GeoServer's configured data directory (e.g. data_dir).
3. Adjust or add a monitor.properties file to the monitoring directory, setting the property 'storage' to 'vector'
```
storage=vector
```
4. Add a new file featureMonitor.properties to the monitoring directory, containing all required properties of a writable vector datastore configured within GeoServer.
The following example is for H2:
```
database=E:\geoserver-2.7.0-bin\geoserver-2.7.0\db
dbtype=h2
```
5. Build the tool
```
% mvn clean install
```
6. Copy the target jar file from 'gsmonitor/target' directory to the GeoServer's web application 'lib' directory.
```
% cp .\gsmonitor\target\gsmonitor-0.0.1-SNAPSHOT.jar  E:\geoserver-2.7.0-bin\webapps\geoserver\WEB-INF\lib
```
