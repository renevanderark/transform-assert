# Transform Assert

This is library is used as a way to assert correctness of XSLT transformations.

It supports the following assertion types:

- String equality of output
- Evaluate output XML using xpath expression
- Validate output XML against an XSD file



## Examples

### String equality (isEqualTo)

```java
TransformAssert.describe(new File("./src/test/resources/1.xslt"))
        .whenTransforming(new File("./src/test/resources/1.xml"))
        .isEqualto("not bar")
        .evaluate();
```

Results in:

```
DESCRIBE:
  ./src/test/resources/1.xslt

WHEN TRANSFORMING:
  ./src/test/resources/1.xml

IT SHOULD:
  EQUAL: not bar (FAILED)

OUTPUT:
  bar
===================================================

FAILURES:
  EQUAL: not bar
    Expected output to equal: 'not bar'
    But got: 'bar'
===================================================
```

### Xpath expressions, using xslt parameters (hasXpathContaining)

```java
TransformAssert.describe(new File("./src/test/resources/5.xslt"))
        .whenTransforming("<root><foo>bar</foo></root>",
            "param1", "param1-value",
            "param2", "param2-value"
        ).hasXpathContaining("/output/one/text()", "bar", "Node <one> moet de tekst binnen <foo> bevatten")
        .andHasXpathContaining("/output/two[1]/text()", "param1-value", "Eerste node <two> moet de waarde van param1 bevatten")
        .andHasXpathContaining("/output/two[2]/text()", "param2-value", "Tweede node <two> moet de waarde van param2 bevatten")
        .evaluate();
```

Results in

```
DESCRIBE:
  ./src/test/resources/5.xslt

WHEN TRANSFORMING:
  <root><foo>bar</foo></root>

IT SHOULD:
  Node <one> moet de tekst binnen <foo> bevatten (OK)
  Eerste node <two> moet de waarde van param1 bevatten (OK)
  Tweede node <two> moet de waarde van param2 bevatten (OK)

OUTPUT:
  <?xml version="1.0" encoding="UTF-8"?>
  <output>
     <one>bar</one>
     <two>param1-value</two>
     <two>param2-value</two>
  </output>
===================================================
```


### Namespaces in xpaths must be declared (usingNamespace)

```java
TransformAssert.describe(new File("./src/test/resources/6.xslt"))
        .whenTransforming("<root><foo>bar</foo></root>")
        .usingNamespace("ns1", "ns1:urn")
        .andUsingNamespace("ns2", "ns2:urn")
        .hasXpathContaining("/ns1:foo/ns2:bar/text()", "bar")
        .evaluate();
```

Results in

```
DESCRIBE:
  ./src/test/resources/6.xslt

WHEN TRANSFORMING:
  <root><foo>bar</foo></root>

IT SHOULD:
  MATCH XPATH /ns1:foo/ns2:bar/text()='bar' (OK)

OUTPUT:
  <?xml version="1.0" encoding="UTF-8"?>
  <ns1:foo xmlns:ns2="ns2:urn" xmlns:ns1="ns1:urn">
     <ns2:bar>bar</ns2:bar>
  </ns1:foo>
===================================================
```

### Validating against XSD (validatesAgainstXSD)

```java
TransformAssert.describe(new File("./src/test/resources/5.xslt"))
        .whenTransforming("<root><foo>bar</foo></root>",
                "param1", "param1-value",
                "param2", "param2-value"
        ).validatesAgainstXSD(new File("src/test/resources/2.xsd"), "node <one> wordt niet verwacht door deze XSD")
        .evaluate();
```

Results in

```
DESCRIBE:
  ./src/test/resources/5.xslt

WHEN TRANSFORMING:
  <root><foo>bar</foo></root>

IT SHOULD:
  node <one> wordt niet verwacht door deze XSD (FAILED)

OUTPUT:
  <?xml version="1.0" encoding="UTF-8"?>
  <output>
     <one>bar</one>
     <two>param1-value</two>
     <two>param2-value</two>
  </output>
===================================================

FAILURES:
  node <one> wordt niet verwacht door deze XSD
    Expected output to validate against XSD: ./src/test/resources/2.xsd
    But got: cvc-complex-type.2.4.a: Invalid content was found starting with element 'one'. One of '{two}' is expected.
===================================================
```

### Log to String Consumer

By default results are logged to standard output using ```System.out.println```. 
However, this behaviour can be overridden in the ```TransformAssert.describe``` method.

Store messages in a list of strings:

```java
final List<String> messages = new ArrayList<>();
describe(new File("./src/test/resources/5.xslt"), messages::add)
        .whenTransforming(XML)
        .evaluate();
```

Print to stderr in stead of stdout:

```java
describe(new File("./src/test/resources/5.xslt"), System.err::println)
        .whenTransforming(XML)
        .evaluate();
```

### Log output to custom String Consumer

When your transformation generates a lot of output you might want to redirect that somewhere else.

Print transformation output to a file:

```java
final PrintWriter pw = new PrintWriter(new FileOutputStream("output.xml"));

describe(new File("./src/test/resources/5.xslt"), System.out::println, pw::println)
        .whenTransforming(XML)
        .evaluate();
``` 