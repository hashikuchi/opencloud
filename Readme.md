#OpenCloud Connection Example
This package is an example of implementation of connection pooling with interfaces given by OpenCloud.

#How to use
ConnectionPoolImpl is the implementation of a connection pool.Caller should use the method of this class.
For details of the class and belonging methods, see the comments of the class file.

##Requirement
These jar files are required to run the product code.
- logback-classic-1.1.3.jar
- logback-core-1.1.3.jar
- slf4j-api-1.7.13.jar

They are included in the lib folder. Please add this file to the build path.

JUnit 4 is required to run the test codes (com.opencloud.test).

##Logging
By default, log files are created in "log" folder. Also the log information is printed into the standard output.
If you want to customize the settings of logging, refer src/com/opencloud/resourcs/logback.xml.