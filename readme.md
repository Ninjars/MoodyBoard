# Moody
This is a utility designed to take lists of twitter links to cool images I'd been building up and turn them into
a format that was actually viewable.

The intended use case is to parse a markdown document consisting of structured links arranged into titled sections 
and optional subsections. Each section will create a Hugo-ingestible markdown page, with subsections delineated within 
the output page.

Example input:
```
## Title
Description paragraph

<link 1>
<link 2>

Section Title
<link a>

## Title 2
Description paragraph

<link x>
<link y>
```

Indicative output:
```
File: title.md
---
title: Title
date: <date stamp>
---
Description paragraph
<!--more-->
Posted by <Author1>
<link 1 image1>
<link 1 image2>

Posted by <Author2>
<link 2 image1>
<link 2 image2>
<link 2 image3>

### Section Title
Posted by <AuthorA>
<link a image1>
<link a image2>
<link a image3>
<link a image4>
```
```
File: title-2.md
---
title: Title2
date: <date stamp>
---
Description paragraph
<!--more-->
Posted by <AuthorX>
<link x image1>

Posted by <AuthorY>
<link y image1>
```
This code is intended to be reasonably easy to adapt or extend: the markdown parsing and output file generation are
separated to facilitate creating, for example, a different input parser for a different structure of data, or an
output writer targeting a different platform. However, the project hasn't been written to be so modular that such 
changes are just plug-and-play with interfaces or the like - the focus was on achieving a result not building a library.

### Running
During dev I've simply been running from within Intellij IDE, using different configurations to parse different documents.

An example of the program arguments used:
```-i "path\to\input\file.md" -o "path\to\output\dir" -t "tags,to,include,in,final,doc"```

`-v` can be appended too for additional logging.

#### Note
Twitter integration requires an API bearer token to be made available. 
This should be made available via a file called `secret.properties` containing the line:
```twitterBearerToken="<token goes here>"```
