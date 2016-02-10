
**Working on your first Pull Request?** You can learn how from this *free* series [How to Contribute to an Open Source Project on GitHub](https://egghead.io/series/how-to-contribute-to-an-open-source-project-on-github)

# How to contribute

Third-party patches are essential for keeping the project going; We are a small team and need your help.
There are a few guidelines that we need contributors to follow so that we can stay on top of things.

## Getting Started

* Make sure you have a [GitHub account](https://github.com/signup/free)
* Submit a ticket for your issue, assuming one does not already exist.
  * Clearly describe the issue including steps to reproduce when it is a bug
  * Make sure you fill in the earliest version that you know has the issue
* Fork the repository on GitHub

## Making Changes

* Create a topic branch from where you want to base your work
  * This is usually the master branch
  * Only target release branches if you are certain your fix must be on that branch
  * To quickly create a topic branch based on master; `git checkout -b
    fix/master/my_contribution master`. Please avoid working directly on the
    `master` branch
* Comment your code
* Create junit tests to demonstrate your work
* Make commits of logical units
* Ensure the code is formatted in-line with our style guide
* Check for unnecessary whitespace with `git diff --check` before committing
* Make sure you have added the necessary tests for your changes
* Run _all_ the tests to assure nothing else was accidentally broken

## Submitting Changes

* Push your changes to a topic branch in your fork of the repository
* Submit a pull request to the repository in which you're working

# Additional Resources

* [General GitHub documentation](https://help.github.com/)
* [GitHub pull request documentation](https://help.github.com/send-pull-requests/)
* [red5 users mailing list](https://groups.google.com/forum/#!forum/red5interest)
* [StackOverflow](http://stackoverflow.com/tags/red5/info)
* [Subreddit](http://www.reddit.com/r/red5)
* [Gitter](https://gitter.im/Red5?utm_source=share-link&utm_medium=link&utm_campaign=share-link)
* [Eclipse Formatting](red5-eclipse-format.xml)

# Supporters
[YourKit](http://www.yourkit.com/) YourKit is kindly supporting open source projects with its full-featured Java Profiler. YourKit, LLC is the creator of innovative and intelligent tools for profiling Java and .NET applications. Take a look at YourKit's leading software products:

[YourKit Java Profiler](http://www.yourkit.com/java/profiler/index.jsp) 
[YourKit .NET Profiler](http://www.yourkit.com/dotnet/index.jsp)

## Donations
Donate to the cause using [Bitcoin](https://coinbase.com/checkouts/2c5f023d24b12245d17f8ff8afe794d3)
<i>Donations are used for beer and snacks</i>

