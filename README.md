Email Decoder
=============

[![Build Status](https://travis-ci.org/chroder/kotlin-email-parser.svg?branch=master)](https://travis-ci.org/chroder/kotlin-email-parser)

This project aims to take any raw email message ([RFC 5322](https://tools.ietf.org/html/rfc5322), 2822, 822)
message and transform it into a _simple data structure_ with outputs to JSON or MsgPack
(or anything else if you implement an ecoder).

The data structures output from this library simplify email parsing for the most common use cases including:

* Getting the HTML and/or text body of an email
* Reading attachments
* Reading and decoding headers like From, To, Subject, Date, etc

Most of the heavy-lifting is handled by [Apache James Mime4j](http://james.apache.org/mime4j/index.html). All we're
doing here is specifying a simple and opinionated data structure.

FAQ
---

**Why not use Mime4j directly?**

Mime4j is the kitchen sink. You still need to put the pieces together and handle error scenarios. The goal of this
library is to give reasonable output for any given email source, so you can write apps that consume email as easily
as you write apps to consume, say, a REST API.

**What do you mean "opinioned" data structure?**

Email is very flexible, and that makes it very complicated. There are many things which are technically possible to
do with email but are never done in practice. This project errs on the side of practical.

Here are a few examples:

* An email may technically be From multiple addresses, but we simplify this and assume there's only one.
* According to the spec, there is a difference between mailboxes, mailbox lists, addresses, and address lists; and
there are esoteric features of mailbox lists that are seldom used in practice (e.g. grouping). We do away with these
complications. An email address is always the same shape no matter what it is (From, To, Sender, Reply-To, whatever).
* We try to normalise and parse common headers into usable data structures. For example, dates are parsed into UTC ISO 8601 dates.
Another exmaple is how we attempt to detect the "Date" based on Recieved headers if there is no Date.
* We simplify parsing out the body html/text, even in convuluted multipart email messages.

The library handles parsing emails and then sorts the various pieces into a structure you can use in real-world
applications.

Example CLI Usage
-----------------

```bash
$ ./io.nade.email.parse -m json --file my-email.eml
# json output (see below for an example)
```

Example Library Usage
---------------------

```kotlin
val file    = File("my-email.eml")
val parser  = Parser.create()
val message = parser.parse(file.inputStream())

val outFile = File("email.json")
val encoder = JsonEncoder.create()
encoder.encodeToStream(message, outFile.outputStream())
```

Example JSON Result
-------------------

```json
{
  "subject": "My Subject",
  "messageId": "\u003cCAOZrWnfF9pMZY4k9stM07W8nCkCoixwEFGK\u003dn710e7xw\u003dd49tw@example.com\u003e",
  "from": {
    "name": "John Doe",
    "email": "john@example.com"
  },
  "sender": null,
  "replyTo": [],
  "returnPath": null,
  "tos": [
    {
      "name": "Jane Doe",
      "email": "jane@example.com"
    }
  ],
  "ccs": [],
  "date": "2017-08-17 13:18:33",
  "references": [],
  "bodyText": "Hello, world!",
  "bodyHtml": "<p>Hello, world!</p>",
  "headers": [
    {
      "name": "MIME-Version",
      "value": "1.0"
    },
    {
      "name": "Received",
      "value": "by 192.168.1.1 with HTTP; Thu, 17 Aug 2017 05:18:33 -0700 (PDT)"
    },
    {
      "name": "X-Originating-IP",
      "value": "[10.0.1.1]"
    },
    {
      "name": "Date",
      "value": "Thu, 17 Aug 2017 13:18:33 +0100",
      "date": "2017-08-17 13:18:33"
    },
    {
      "name": "Delivered-To",
      "value": "jane@example.com"
    },
    {
      "name": "Message-ID",
      "value": "\u003cCAOZrWnfF9pMZY4k9stM07W8nCkCoixwEFGK\u003dn710e7xw\u003dd49tw@example.com\u003e"
    },
    {
      "name": "Subject",
      "value": "My Subject"
    },
    {
      "name": "From",
      "value": "John Doe \u003cjohn@example.com\u003e",
      "addresses": [
        {
          "name": "John Doe",
          "email": "john@example.com"
        }
      ]
    },
    {
      "name": "To",
      "value": "Jane Doe \u003cjane@example.com\u003e",
      "addresses": [
        {
          "name": "Jane Doe",
          "email": "jane@example.com"
        }
      ]
    },
    {
      "name": "Content-Type",
      "value": "multipart/mixed; boundary\u003d\"001a113eb2a6dace510556f2022f\""
    }
  ]
}
```