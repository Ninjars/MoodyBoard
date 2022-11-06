# Moody
This is a utility designed to take lists of twitter links to cool images I'd been building up and turn them into
a format that was actually viewable.

The intended use case is to parse a markdown document consisting of lists of links separated into sections
in this structure:

```
<Page>
<SectionTitle>
<link 1>
<link 2>
<...>

<SectionTitle>
<link a>
<link b>
<...>
```

And to produce a Hugo-ingestible markdown page displaying images extracted from the links with links back to the source 
and reference to the author.

### Running
During dev I've simply been running from within Intellij IDE, using different configurations to parse different documents.

An example of the program arguments used:
```-i "path\to\input\file.md" -o "path\to\output\dir" -t "tags,to,include,in,final,doc" -n "Page Name"```

`-v` can be appended too for additional logging.

#### Note
Twitter integration requires an API bearer token to be made available. 
This should be made available via a file called `secret.properties` containing the line:
```twitterBearerToken="<token goes here>"```
