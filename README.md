# timeronsubprocess

Small process to demonstrate the issue with timerboundary events on subprocess.

![Image of process](https://github.com/thuri/timeronsubprocess/blob/master/timeronsubprocess/pictures/Process.PNG)

Behind the "Call Camel Route" Activity there is a Camel Route which needs 2 times the time duration defined on the timerboundary event on the subprocess.

The JUnit tests demonstrates that the "Should never be called" will be executed after the Camel Route of the Activity before it returns. 
That should not happen because the timer should have been reached and the subprocess should have been canceled.
