package com.ge.predix.solsvc.winddata.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.PostConstruct;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.http.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.ImportResource;
import org.springframework.stereotype.Component;

import com.ge.predix.entity.timeseries.datapoints.ingestionrequest.Body;
import com.ge.predix.entity.timeseries.datapoints.ingestionrequest.DatapointsIngestion;
import com.ge.predix.entity.timeseries.datapoints.queryrequest.DatapointsQuery;
import com.ge.predix.entity.timeseries.datapoints.queryrequest.latest.DatapointsLatestQuery;
import com.ge.predix.entity.timeseries.datapoints.queryresponse.DatapointsResponse;
import com.ge.predix.entity.timeseries.tags.TagsList;
import com.ge.predix.solsvc.spi.IServiceManagerService;
import com.ge.predix.solsvc.timeseries.bootstrap.client.TimeseriesClient;
import com.ge.predix.solsvc.timeseries.bootstrap.config.ITimeseriesConfig;
import com.ge.predix.solsvc.winddata.api.SubstationFeeder;
import com.ge.predix.solsvc.winddata.api.WindDataAPI;

/**
 * Return data over REST from time series for PredixNodejsStarter
 * 
 * To turn on a 2nd time series:
 * 1. uncomment the 3 sections below
 * 2. in SecondaryTimeseriesConfig uncomment the @Component
 * 3. in src/main/resources/application-cloud.properties uncomment the props for Option 1 or 2
 * 4. in manifest.yml uncomment and fill in the properties for Option 1 or 2
 * 
 * @author predix -
 */
@Component
@ImportResource(
{
        "classpath*:META-INF/spring/timeseries-bootstrap-scan-context.xml"
})
public class WindDataImpl
        implements WindDataAPI
{
    private static Logger          log = LoggerFactory.getLogger(WindDataImpl.class);

    @Autowired
    private IServiceManagerService serviceManagerService;

    @Autowired
    private TimeseriesClient       timeseriesClient;

    @Autowired
    @Qualifier("defaultTimeseriesConfig")
    private ITimeseriesConfig      timeseriesConfig;

    // things needed for 2nd time series, if you have that use-case
    // @Autowired
    // private RestClient restClient2;
    //
    // @Autowired
    // private TimeseriesFactory timeseriesFactory2;
    //
    // @Autowired
    // private SecondaryTimeseriesConfig secondaryTimeseriesConfig;

    /**
     * -
     */
    public WindDataImpl()
    {
        super();
    }

    /**
     * -
     */
    @SuppressWarnings("nls")
    @PostConstruct
    public void init()
    {
        try
        {
            this.serviceManagerService.createRestWebService(this, null);
            this.timeseriesClient.createTimeseriesWebsocketConnectionPool();
            createMetrics();
        }
        catch (Exception e)
        {
            throw new RuntimeException(
                    "unable to set up timeseries Websocket Pool timeseriesConfig=" + this.timeseriesConfig, e);
        }
        // try {
        // // a second timeseries, if you have that use-case
        // this.restClient2.overrideRestConfig(this.secondaryTimeseriesConfig);
        // this.timeseriesFactory2.overrideConfig(this.secondaryTimeseriesConfig);
        // log.debug("init secondary timeseries properties=" + this.secondaryTimeseriesConfig.toString());
        // // if write privs to 2nd timeseries are revoked do not uncomment
        // // this or you'll get 401 unauthorized
        // // this.timeseriesFactory2.createConnectionToTimeseriesWebsocket();
        // createMetrics();
        // } catch (Exception e) {
        // throw new RuntimeException("unable to set up timeseries Websocket Pool secondaryTimeseriesConfig="
        // + this.secondaryTimeseriesConfig, e);
        // }
    }

    @Override
    public Response greetings()
    {
        return handleResult("Greetings from CXF Bean Rest Service " + new Date()); //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     * @see com.ge.predix.solsvc.api.WindDataAPI#getWindDataTags()
     */
    @SuppressWarnings("nls")
    @Override
    public Response getWindDataTags(String authorization)
    {
        try
        {
            List<Header> headers = this.timeseriesClient.getTimeseriesHeaders();
            TagsList tagsList = this.timeseriesClient.listTags(headers);

            // example of calling 2nd time series
            // List<Header> headers2 = this.restClient2.getSecureTokenForClientId();
            // this.restClient2.addZoneToHeaders(headers2, this.secondaryTimeseriesConfig.getZoneId());
            // TagsList tagsList2 = this.timeseriesFactory2.listTags(headers2);
            // tagsList.getResults().add("*****************************");
            // tagsList.getResults().add(this.timeseriesConfig.toString());
            // tagsList.getResults().add("*****************************");
            // tagsList.getResults().add(this.secondaryTimeseriesConfig.toString());
            // tagsList.getResults().add("*****************************");
            // tagsList.getResults().addAll(tagsList2.getResults());

            return handleResult(tagsList);
        }
        catch (Throwable e)
        {
            log.error("unable to get wind data, config=" + this.timeseriesConfig, e);
            // This is sample code so we need to easily show you what went
            // wrong, please convert your app to show appropriate info to end
            // users. For security
            // reasons do not expose these properties.
            throw new RuntimeException(
                    "unable to get wind data, errorMsg=" + e.getMessage() + ". config=" + this.timeseriesConfig, e);
        }
    }

    @SuppressWarnings("nls")
    @Override
    public Response getYearlyWindDataPoints(String id, String authorization, String starttime, String taglimit,
            String tagorder)
    {
        try
        {
            if ( id == null )
            {
                return null;
            }

            List<Header> headers = this.timeseriesClient.getTimeseriesHeaders();

            DatapointsQuery dpQuery = buildDatapointsQueryRequest(id, starttime, getInteger(taglimit), tagorder);
            DatapointsResponse response = this.timeseriesClient.queryForDatapoints(dpQuery, headers);
            log.debug(response.toString());
            return handleResult(response);
        }
        catch (Throwable e)
        {
            log.error("unable to get wind data, config=" + this.timeseriesConfig, e);
            // This is sample code so we need to easily show you what went
            // wrong, please convert your app to show appropriate info to end
            // users. For security
            // reasons do not expose these properties.
            throw new RuntimeException(
                    "unable to get wind data, errorMsg=" + e.getMessage() + ". config=" + this.timeseriesConfig, e);
        }
    }

    /**
     * 
     * @param s
     *            -
     * @return
     */
    private int getInteger(String s)
    {
        int inValue = 25;
        try
        {
            inValue = Integer.parseInt(s);

        }
        catch (NumberFormatException ex)
        {
            // s is not an integer
        }
        return inValue;
    }

    @SuppressWarnings("nls")
    @Override
    public Response getLatestWindDataPoints(String id, String authorization)
    {
        try
        {
            if ( id == null )
            {
                return null;
            }
            List<Header> headers = this.timeseriesClient.getTimeseriesHeaders();

            DatapointsLatestQuery dpQuery = buildLatestDatapointsQueryRequest(id);
            DatapointsResponse response = this.timeseriesClient.queryForLatestDatapoint(dpQuery, headers);
            log.debug(response.toString());

            return handleResult(response);
        }
        catch (Throwable e)
        {
            log.error("unable to get wind data, config=" + this.timeseriesConfig, e);
            // This is sample code so we need to easily show you what went
            // wrong, please convert your app to show appropriate info to end
            // users. For security
            // reasons do not expose these properties.
            throw new RuntimeException(
                    "unable to get wind data, errorMsg=" + e.getMessage() + ". config=" + this.timeseriesConfig, e);

        }
    }

    private DatapointsLatestQuery buildLatestDatapointsQueryRequest(String id)
    {
        DatapointsLatestQuery datapointsLatestQuery = new DatapointsLatestQuery();

        com.ge.predix.entity.timeseries.datapoints.queryrequest.latest.Tag tag = new com.ge.predix.entity.timeseries.datapoints.queryrequest.latest.Tag();
        tag.setName(id);
        List<com.ge.predix.entity.timeseries.datapoints.queryrequest.latest.Tag> tags = new ArrayList<com.ge.predix.entity.timeseries.datapoints.queryrequest.latest.Tag>();
        tags.add(tag);
        datapointsLatestQuery.setTags(tags);
        return datapointsLatestQuery;
    }

    /**
     * 
     * @param id
     * @param startDuration
     * @param tagorder
     * @return
     */
    private DatapointsQuery buildDatapointsQueryRequest(String id, String startDuration, int taglimit, String tagorder)
    {
        DatapointsQuery datapointsQuery = new DatapointsQuery();
        List<com.ge.predix.entity.timeseries.datapoints.queryrequest.Tag> tags = new ArrayList<com.ge.predix.entity.timeseries.datapoints.queryrequest.Tag>();
        datapointsQuery.setStart(startDuration);
        // datapointsQuery.setStart("1y-ago"); //$NON-NLS-1$
        String[] tagArray = id.split(","); //$NON-NLS-1$
        List<String> entryTags = Arrays.asList(tagArray);

        for (String entryTag : entryTags)
        {
            com.ge.predix.entity.timeseries.datapoints.queryrequest.Tag tag = new com.ge.predix.entity.timeseries.datapoints.queryrequest.Tag();
            tag.setName(entryTag);
            tag.setLimit(taglimit);
            tag.setOrder(tagorder);
            tags.add(tag);
        }
        datapointsQuery.setTags(tags);
        return datapointsQuery;
    }

    @SuppressWarnings(
    {
            "nls", "unchecked"
    })
    private void createMetrics() {

			DatapointsIngestion dpIngestion = new DatapointsIngestion();
			dpIngestion.setMessageId(String.valueOf(System.currentTimeMillis()));
			
			Body body = new Body();
			body.setName("Malahide Castle:MC14:watts"); //$NON-NLS-1$
			
			File CSV_file = new File("E:/predix/PredixProjects/minds-machines-sf/Electrification Challenge/Grid Timeseries Dataset/LoadForecasting/LoadData/L3_Power_MC12.csv");
			/*FileInputStream fs = new FileInputStream(CSV_file);
			Workbook wb = Workbook.getWorkbook(fs);*/

			body.setDatapoints(processInputFile(CSV_file));
			//body.setDatapoints(processWeatherFile(CSV_file));

			com.ge.predix.entity.util.map.Map map = new com.ge.predix.entity.util.map.Map();
			map.put("WeatherStationNo", "1332"); //$NON-NLS-2$
			map.put("LoadType", "Ind/Comm"); //$NON-NLS-2$
			map.put("Voltage", "10"); //$NON-NLS-2$
			
			body.setAttributes(map);

			List<Body> bodies = new ArrayList<Body>();
			bodies.add(body);

			dpIngestion.setBody(bodies);
			this.timeseriesClient.postDataToTimeseriesWebsocket(dpIngestion);
	}
   /* private void createMetrics() {

		DatapointsIngestion dpIngestion = new DatapointsIngestion();
		dpIngestion.setMessageId(String.valueOf(System.currentTimeMillis()));
		
		Body body = new Body();
		body.setName("Letterkenny-Dromore:newtest:"); //$NON-NLS-1$
		File CSV_file = new File("E:/predix/PredixProjects/minds-machines-sf/Electrification Challenge/Grid Timeseries Dataset/LoadForecasting/WeatherData/hly1043test.csv");
				
		//body.setDatapoints(processInputFile(CSV_file));
		body.setDatapoints(processWeatherFile(CSV_file));

		com.ge.predix.entity.util.map.Map map = new com.ge.predix.entity.util.map.Map();
		map.put("WeatherStationNo", "1043"); //$NON-NLS-2$
		map.put("Station Height(m)", "20"); //$NON-NLS-2$
		map.put("Latitude", "54.942"); //$NON-NLS-2$
		map.put("Longitude", "-7.6797");
		body.setAttributes(map);

		List<Body> bodies = new ArrayList<Body>();
		bodies.add(body);

		dpIngestion.setBody(bodies);
		this.timeseriesClient.postDataToTimeseriesWebsocket(dpIngestion);
}*/
    /*private void createMetrics()
    {
        for (int i = 0; i < 10; i++)
        {
            DatapointsIngestion dpIngestion = new DatapointsIngestion();
            dpIngestion.setMessageId(String.valueOf(System.currentTimeMillis()));

            Body body = new Body();
            body.setName("Compressor-2020:CompressionRatio"); //$NON-NLS-1$
            List<Object> datapoint1 = new ArrayList<Object>();
            datapoint1.add(generateTimestampsWithinYear(System.currentTimeMillis()));
            datapoint1.add(10);
            datapoint1.add(3); // quality

            List<Object> datapoint2 = new ArrayList<Object>();
            datapoint2.add(generateTimestampsWithinYear(System.currentTimeMillis()));
            datapoint2.add(9);
            datapoint2.add(1); // quality

            List<Object> datapoint3 = new ArrayList<Object>();
            datapoint3.add(generateTimestampsWithinYear(System.currentTimeMillis()));
            datapoint3.add(27);
            datapoint3.add(0); // quality

            List<Object> datapoint4 = new ArrayList<Object>();
            datapoint4.add(generateTimestampsWithinYear(System.currentTimeMillis()));
            datapoint4.add(78);
            datapoint4.add(2); // quality

            List<Object> datapoint5 = new ArrayList<Object>();
            datapoint5.add(generateTimestampsWithinYear(System.currentTimeMillis()));
            datapoint5.add(2);
            datapoint5.add(3); // quality

            List<Object> datapoint6 = new ArrayList<Object>();
            datapoint6.add(generateTimestampsWithinYear(System.currentTimeMillis()));
            datapoint6.add(98);
            datapoint6.add(1); // quality

            List<Object> datapoints = new ArrayList<Object>();
            datapoints.add(datapoint1);
            datapoints.add(datapoint2);
            datapoints.add(datapoint3);
            datapoints.add(datapoint4);
            datapoints.add(datapoint5);
            datapoints.add(datapoint6);

            body.setDatapoints(datapoints);

            com.ge.predix.entity.util.map.Map map = new com.ge.predix.entity.util.map.Map();
            map.put("host", "server1"); //$NON-NLS-2$
            map.put("customer", "Acme"); //$NON-NLS-2$

            body.setAttributes(map);

            List<Body> bodies = new ArrayList<Body>();
            bodies.add(body);

            dpIngestion.setBody(bodies);
            this.timeseriesClient.postDataToTimeseriesWebsocket(dpIngestion);
        }
    }*/

    @SuppressWarnings("javadoc")
    protected Response handleResult(Object entity)
    {
        ResponseBuilder responseBuilder = Response.status(Status.OK);
        responseBuilder.type(MediaType.APPLICATION_JSON);
        responseBuilder.entity(entity);
        return responseBuilder.build();
    }

    private Long generateTimestampsWithinYear(Long current)
    {
        long yearInMMS = Long.valueOf(31536000000L);
        return ThreadLocalRandom.current().nextLong(current - yearInMMS, current + 1);
    }
    
    private List<Object> processInputFile(File inputFilePath) {
	    List<Object> feeder = new ArrayList<Object>();
	    try{
	    	
	      //File inputF = new File(inputFilePath);
	      BufferedReader reader = new BufferedReader(new FileReader(inputFilePath));
	      
	   // read file line by line
		   String line = null;
		   Scanner scanner = null;
		   int index = 0, cnt=0;
		   
		   while ((line = reader.readLine()) != null) {
			    List<Object> datapt = new ArrayList<Object>();
				//SubstationFeeder datapt = new SubstationFeeder();
				scanner = new Scanner(line);
				scanner.useDelimiter(",");
				cnt++;
				while (scanner.hasNext()) {				
					String data = scanner.next();
					if (index == 0)
						datapt.add(Long.parseLong(data));
					else if (index == 1) {
						datapt.add(Integer.parseInt(data));	
					    datapt.add(3);	
					}
					else
						System.out.println("invalid data::" + data);
					index++;
				}
				index = 0;
				feeder.add(datapt);
			}
	      
	      reader.close();
	     // System.out.println(feeder);
	      System.out.println("******************************************************");
	      System.out.println("******************************************************");
	      System.out.println("******************************************************");
	      System.out.println("******************************************************");
		  System.out.println(cnt);
	    } catch (IOException e) {
	    	e.printStackTrace();
	    }
	    return feeder ;
	}
    
    private List<Object> processWeatherFile(File inputFilePath) {
	    List<Object> weathervar = new ArrayList<Object>();
	    try{
	    	
	      //File inputF = new File(inputFilePath);
	      BufferedReader reader = new BufferedReader(new FileReader(inputFilePath));
	      
	   // read file line by line
		   String line = null;
		   Scanner scanner = null;
		   int index = 0, lineno=0; // have Skip first colum of time .
		   
		   while ((line = reader.readLine()) != null) {
			   lineno++;
			   System.out.println(" ******************LINE:" + lineno);
			    if (lineno < 22) {
			    	
			    	continue; //Ignore first 21 lines.
			    }
				if (lineno > 18022) {

					break; // Ignore first 21 lines.
				}
			    List<Object> datapt = new ArrayList<Object>();
				//SubstationFeeder datapt = new SubstationFeeder();
				scanner = new Scanner(line);
				scanner.useDelimiter(",");
				
				while (scanner.hasNext()) {						
					String data = scanner.next();
					if (index == 0) {
						//System.out.println(" Skipe first column");
						index++;
					    continue;
					}
					else if (index == 1)
						datapt.add(Long.parseLong(data));
					else if (index == 2) {
						datapt.add(Integer.parseInt(data));	
					    datapt.add(3);	
				//	    System.out.println(" ingested");
					}
					else
						System.out.println("invalid data::" + data);
					
					if (index==2) {
						break;
					}
					
					index++;
				}
				index = 0;
				weathervar.add(datapt);
				
			}
	      
	      reader.close();
	     // System.out.println(feeder);
	      System.out.println("******************************************************");
	      System.out.println("******************************************************");
	      System.out.println("******************************************************");
	      System.out.println("******************************************************");
		  //System.out.println(cnt);
	    } catch (IOException e) {
	    	e.printStackTrace();
	    }
	    return weathervar ;
	}

}
