package timeronsubprocess;

import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.repository.DeploymentBuilder;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.Period;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:context.xml")
public class SubprocessWithTimerTest {

  private static final Logger logger = LogManager.getLogger();
  
  @Autowired
  @Qualifier("runtimeService")
  private RuntimeService runtimeService;
  
  @Autowired
  @Qualifier("repositoryService")
  private RepositoryService repoService;
  
  @Autowired
  @Qualifier("historyService")
  private HistoryService historyService;
  
  @Autowired
  @Qualifier("camelContext")
  private CamelContext camelContext;
  
  @Produce(uri = "activiti:myProcess?copyVariablesFromProperties=true")
  private ProducerTemplate startProcess;
  
  @EndpointInject(uri = "mock:mockEndpoint")
  private MockEndpoint longRunningMock;
  
  @EndpointInject(uri = "mock:shouldNeverBeCalledMock")
  private MockEndpoint shouldNeverBeCalledMock;
  
  @EndpointInject(uri = "mock:timeoutHandlerMock")
  private MockEndpoint timeoutHandlerMock;
  
  @Test
  public void test() throws Exception {
    
    Period maxWaitTime = Period.millis(5000);
    /*
     * Arrange
     */
    DeploymentBuilder deployment = repoService.createDeployment();
    deployment.addClasspathResource("Process.bpmn");
    deployment.deploy();
    
    camelContext.addRoutes(new RouteBuilder() {

      @Override
      public void configure() throws Exception {
        from("activiti:myProcess:callCamel")
          .to(longRunningMock);
        from("activiti:myProcess:shouldNeverBeCalled")
          .to(shouldNeverBeCalledMock);
        from("activiti:myProcess:timeoutHandler")
          .to(timeoutHandlerMock);
      }
    });
    
    longRunningMock.setExpectedMessageCount(1);
    longRunningMock.whenAnyExchangeReceived(new Processor(){
      @Override
      public void process(Exchange arg0) throws Exception {
        long waitTime=maxWaitTime.getMillis() * 2;
        logger.debug("start waiting in long running route. Will wait for "+waitTime);
        Thread.sleep(waitTime);
        logger.debug("finished waiting in long running route");
      }
    });
    
    shouldNeverBeCalledMock.setExpectedMessageCount(0);
    
    timeoutHandlerMock.setResultWaitTime(maxWaitTime.getMillis() * 3);
    timeoutHandlerMock.setExpectedMessageCount(1);
    
    logger.debug("Starting process. Passing maxWaitTime="+maxWaitTime.toString());
    
    startProcess.sendBodyAndProperty("", "maxWaitTime", maxWaitTime.toString());
    
    longRunningMock.assertIsSatisfied();
//    shouldNeverBeCalledMock.assertIsSatisfied();
    timeoutHandlerMock.assertIsSatisfied();
  }

}
