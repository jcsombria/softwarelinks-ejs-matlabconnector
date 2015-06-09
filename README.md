# softwarelinks-ejs-matlabconnector
Connect Easy Java Simulations with Matlab.

## Introduction
Sofwarelinks is a set of libraries that add Easy Java Simulations interoperability with external engineering software. The aim is to have a platform/software-independent API to control remote simulations or real systems to develop remote/virtual labs.

The interoperability API provides several primitive methods which are commonly needed to communicate with remote labs:
- connect(): Creates a new connection.
- disconnect(): Terminates an existent connection.
- eval(command): Evaluates a Matlab code.
- set(variable, value): Set/define the value of a Matalb variable into the Matlab workspace.
- get(variable): Get the value of a variable from the Matlab workspace.

The ejs-matlabconnector is composed of the following elements:
- The EJS MatlabConnector Element: An EJS plugin implementing the Matlab RPC Client.
- The RPC Matlab Server: A Matlab RPC Server.
- Third-party libraries:
  - matlabcontrol (https://code.google.com/p/matlabcontrol/): is a Java API that allows for calling MATLAB from Java: spawn Matlab sessions, evaluate code and read/write variables to the workspace, etc.

## Installation
To install the EJS MatlabConnector Element, copy the folder 'bin/softwarelinks/' to '$EJS/bin/extensions/model_elements'. It will be automatically load the next time EJS is opened.

The RPC Matlab Server is contained in a runnable jar file 'bin/RpcMatlabServer.jar', so you only have to copy to your local drive and run it. By default, the server listen to the port 2055.

## EJS Example
A basic example of use in EJS. A senoidal function `y(t)` is evaluated in Matlab, passing the time `t` as parameter, which is generated in EJS.

First, go to the Model Elements page and add the EJS MatlabConnector element to the simulation, with instance name 'matlab'.

Then, the connection is established inside an initialization page:
```
// Initialization page
matlab.connect()
matlab.set("f=1");
```
Finally, to evaluated the function with a fixed period `dt`, the following code is added to a new evolution page:
```
// Evolution page
t += dt; 
matlab.set("t", t);
matlab.eval("y=sin(2*Math.PI*f*t)");
y = matlab.get("y");
```
