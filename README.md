## Javactl - Controlling java daemons
Javactl is a work in progress command line utility that helps with the creation and management of java daemons

### What is the goal of this project?
Java lacks ways to integrate as a service like other linux native applications, our goal is to provide an easy and manageable way to integrate a service or server written in java as a daemon.

### How does it work?
Javactl creates a custom systemd service file and socket for your java daemon, for special applications that may still need user input you can "connect" to a running daemon, and you will gain access to an interactive console allowing you to send user input to the application without the need of a screen.

### Installing
TODO

### Basic usage
Running `javactl` without any argument will perform a basic environment check to see if it's compatible with your system. Running `javactl --help` will provide detailed information about how to use javactl.

## Todo list
- [ ] Improve `javactl connect` virtual terminal.
- [x] A way to list all java daemons.  
- [ ] `javactl install` to allow the installation of java programs that provides metadata about how it should be installed.
- [ ] Better error messages and handling.
- [x] Wrapper for common systemctl commands.
- [ ] Better root detection.
- [ ] Support for colors on `javactl connect`.
- [ ] Custom ppa and native image for release.  
- [ ] Clean up before full release.