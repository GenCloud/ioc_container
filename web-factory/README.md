
## Module 'web-factory'
[![Build Status](https://travis-ci.org/GenCloud/ioc_container.svg?branch=master)](https://travis-ci.org/GenCloud/ioc_container)

### Intro
Add web-factory module to your project. for maven projects just add this dependency:
```xml
   <repositories>
       <repository>
           <id>ioc_cache</id>
           <url>https://raw.github.com/GenCloud/ioc_container/web</url>
           <snapshots>
               <enabled>true</enabled>
               <updatePolicy>always</updatePolicy>
           </snapshots>
       </repository>
   </repositories>
    
   <dependencies>
       <dependency>
           <groupId>org.ioc</groupId>
           <artifactId>web-factory</artifactId>
           <version>2.1.0.STABLE</version>
       </dependency>
   </dependencies>
```
    
A typical use of web-factory module would be:
1) Add in Main class of application marker-annotation of enabled this module
```java
    @WebModule
    @ScanPackage(packages = {"org.ioc.test"})
    public class MainTest {
        public static void main(String... args){
          IoCStarter.start(MainTest.class, args);
        }
    }
```

* default configurations for web server
```properties
# Web server
web.server.port=8081
web.server.ssl-enabled=false
web.server.velocity.input.encoding=UTF-8
web.server.velocity.output.encoding=UTF-8
web.server.velocity.resource.loader=file
web.server.velocity.resource.loader.class=org.apache.velocity.runtime.resource.loader.FileResourceLoader
web.server.velocity.resource.loading.path=./site
```

2) Mark sample controller of @IoCController
```java
    @IoCController
    @UrlMapping //default marker for mapping requests
    public class SampleController implements ContextSensible, DestroyProcessor {
    	@UrlMapping("/") //mapping request on path "site.com/"
    	//ModelAndView - map with needed attributes in page
    	public ModelAndView index() {
    		final ModelAndView modelAndView = new ModelAndView();
    		final File directory = new File(home);
    
    		final List<SampleEntity> sampleEntities = databaseComponent.findAll();
    
    		final List<TypeMetadata> converted = new ArrayList<>();
    
    		Map<String, TypeMetadata> proto = context.getPrototypeFactory().getTypes();
    
    		Map<String, TypeMetadata> sing = context.getSingletonFactory().getTypes();
    
    		Map<String, TypeMetadata> req = context.getRequestFactory().getTypes();
    
    		converted.addAll(proto.values());
    		converted.addAll(sing.values());
    		converted.addAll(req.values());
    
    		modelAndView.addAttribute("types", converted);
    		modelAndView.addAttribute("entities", sampleEntities);
    		modelAndView.addAttribute("dir", directory);
    		modelAndView.setView("index");
    
    		return modelAndView;
    	}
    
    	@UrlMapping("/date")
    	//DateFormatter - mandatory annotation with your date reading format from request
    	//Param - mandatory annotation for reading request attribute name
    	public IMessage testDate(@DateFormatter("yyyy-MM-dd HH:mm") @Param("date") Date date) {
    		return new IMessage(date.toString());
    	}
    
    	@UrlMapping(value = "/upload", method = POST)
    	public IMessage upload(File file) {
    		if (file == null) {
    			return new IMessage(IMessage.Type.ERROR, "Can't upload");
    		}
    
    		File directory = new File(home);
    		if (!directory.exists()) {
    			directory.mkdir();
    		}
    
    		File newFile = new File(home + file.getName());
    		file.renameTo(newFile);
    
    		return new IMessage("Uploaded: " + file.getName());
    	}
    
    	@UrlMapping("/download")
    	public File download(HttpRequest request) {
    		QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
    
    		return new File(home + decoder.path().substring(10));
    	}
    
    	@UrlMapping("/remove")
    	public IMessage remove(@Param("name") String name) {
    		File directory = new File(home);
    		if (!directory.exists()) {
    			return new IMessage(IMessage.Type.ERROR, "File don't exists");
    		}
    
    		File[] files = directory.listFiles((dir, filterName) -> name.equals(filterName));
    
    		if (files == null || files.length == 0) {
    			return new IMessage(IMessage.Type.ERROR, "File don't exists");
    		}
    
    		return files[0].delete() ? new IMessage("Deleted") : new IMessage(IMessage.Type.ERROR, "Delete error");
    	}
    
    	@UrlMapping("/clear")
    	public IMessage clear() {
    		File directory = new File(home);
    		if (directory.exists()) {
    			Arrays.stream(Objects.requireNonNull(directory.listFiles())).forEach(File::delete);
    			directory.delete();
    		}
    
    		return new IMessage("Successful cleared");
    	}
    
    	@Override
    	public void destroy() {
    		databaseComponent.findAll().forEach(e -> databaseComponent.deleteSampleEntity(e));
    	}
    }
```
* page template - default usage Velocity

3) Define sample page in configured directory:
```vtl
<html>
<head>
    <meta charset="utf-8"/>
    <title>Netty Server</title>
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css"
          integrity="sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u" crossorigin="anonymous">

    <link rel="stylesheet" href="/static/css/style.css"/>
    <link rel="stylesheet" href="/static/css/pnotify.custom.min.css"/>
    <link rel="stylesheet" href="/static/css/pnotify.css"/>
    <link rel="stylesheet" href="/static/css/pnotify.buttons.css"/>
</head>
<body>
<div class="container">
    <h1>Netty Server</h1>
    <br>
    <h4>Test uploading file</h4>
    <br>
    <form method="post">
        <button type="button" class="btn btn-info">Upload File</button>
        <input type="file" name="file" class="hide"/>
        <button type="button" class="btn btn-success">Clear All Files</button>
    </form>

    <h4>Test parsing date format</h4>
    <br>
    <form id="form" method="get">
        <input name="date" id="date" type="hidden" value="2018-10-13 23:40"/>

        <button type="button" class="btn btn-danger">Test Date</button>
    </form>

    <table class="table">
        <tr>
            <th>File Name</th>
            <th>File Size</th>
            <th>Option's</th>
        </tr>

        #foreach($item in $!dir.listFiles())
            <tr>
                <td><a href="/download/$item.name">$item.name</a></td>
                <td>
                    #set($kb = $item.length())
                    $kb bytes
                </td>
                <td>
                    <button class="btn btn-warning" name="$item.name">Delete</button>
                </td>
            </tr>
        #end
    </table>

    <h4>Test orm data</h4>
    <br>
    <table class="table">
        <tr>
            <th>#</th>
            <th>Entity</th>
            <th>OneToMany relation (size elements)</th>
        </tr>

        #foreach($item in $!entities)
            <tr>
                <td>$item.id</td>
                <td>$item.name</td>
                #if($item.childEntities.size() > 0)
                    <td>$item.childEntities</td>
                #else
                    <td>List empty</td>
                #end
            </tr>
        #end
    </table>

    <h4>Statistic context</h4>
    <br>
    <table class="table">
        <tr>
            <th>Loading mode</th>
            <th>Name</th>
            <th>Instance</th>
        </tr>

        #foreach($item in $!types)
            <tr>
                <td>$item.mode</td>
                <td>$item.name</td>
                <td>$item.instance</td>
            </tr>
        #end
    </table>
</div>

<script type="text/javascript" src="/static/js/jquery.js"></script>
<script type="text/javascript" src="/static/js/bootstrap.min.js"></script>
<script type="text/javascript" src="/static/js/scripts.js"></script>
<script type="text/javascript" src="/static/js/pnotify.js"></script>
<script type="text/javascript" src="/static/js/pnotify.buttons.js"></script>

</body>
</html>
```
4) Start your app or tests and see results on default path (http://127.0.0.1:8081)
![http://screenshot.ru/7e580e04b2a3e752b176298ea4b5cca0.png](http://screenshot.ru/7e580e04b2a3e752b176298ea4b5cca0.png)

5) Use it!

### Contribute
Pull requests are welcomed!!

The license is [GNU GPL V3](https://www.gnu.org/licenses/gpl-3.0.html/).

_GenCloud_
