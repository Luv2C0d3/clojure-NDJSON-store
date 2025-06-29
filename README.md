# NDJSON Repository

A Clojure library for NDJSON-based repositories with O(1) lookups through in-memory indexing.

## Features

- Fast O(1) lookups through in-memory indexing
- Persistent storage using NDJSON format
- Support for multiple primary keys
- Automatic file synchronization
- Type-safe through generics
- Can be extended or composed into specific repositories

## Installation

### Leiningen/Boot
```clojure
[org.clojars.luv2c0d3/ndjson-repository "0.1.0-SNAPSHOT"]
```

### Maven
```xml
<dependency>
  <groupId>org.clojars.luv2c0d3</groupId>
  <artifactId>ndjson-repository</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### Gradle
```groovy
// Groovy DSL
implementation 'org.clojars.luv2c0d3:ndjson-repository:0.1.0-SNAPSHOT'

// Kotlin DSL
implementation("org.clojars.luv2c0d3:ndjson-repository:0.1.0-SNAPSHOT")
```

Make sure you have the Clojars repository in your build configuration:

Maven (`pom.xml`):
```xml
<repositories>
  <repository>
    <id>clojars</id>
    <name>Clojars</name>
    <url>https://repo.clojars.org/</url>
  </repository>
</repositories>
```

Gradle (`build.gradle` or `build.gradle.kts`):
```groovy
// Groovy DSL
repositories {
    maven {
        url "https://repo.clojars.org"
    }
}

// Kotlin DSL
repositories {
    maven {
        url = uri("https://repo.clojars.org")
    }
}
```

## Usage

The library provides a generic NDJSON repository that can be extended or composed into specific repositories. Here's a complete example showing how to create a repository for managing bears and grizzlies:

```clojure
(ns my.bears
  (:require [org.clojars.luv2c0d3.ndjson-repository.ndjson :refer [create-repository find-by-key 
                                                                   add! delete! 
                                                                   add-to-atom! delete-from-atom!]]
            [clojure.string :as str]))

;; Create a repository with two primary keys: :bear and :grizzly
(defn create-bears-repository
  ([] (create-bears-repository "resources/bears.nosql"))
  ([filename]
   (create-repository filename [:bear :grizzly])))

;; Version 1: Explicit state management
(defn store-bear!
  [repo bear-id client-id scope expires-at]
  (let [bear-entry {:bear bear-id
                    :client_id client-id
                    :scope (str/split scope #" ")
                    :expires_at expires-at}
        new-repo (add! repo bear-entry)]
    [new-repo bear-entry]))

;; Version 2: Using atom-aware functions
(defn store-bear-atom!
  [repo-atom bear-id client-id scope expires-at]
  (let [bear-entry {:bear bear-id
                    :client_id client-id
                    :scope (str/split scope #" ")
                    :expires_at expires-at}]
    (add-to-atom! repo-atom bear-entry)))

;; Similarly for grizzlies...
(defn store-grizzly!
  [repo grizzly-id client-id scope]
  (let [grizzly-entry {:grizzly grizzly-id
                       :client_id client-id
                       :scope (str/split scope #" ")}
        new-repo (add! repo grizzly-entry)]
    [new-repo grizzly-entry]))

(defn store-grizzly-atom!
  [repo-atom grizzly-id client-id scope]
  (let [grizzly-entry {:grizzly grizzly-id
                       :client_id client-id
                       :scope (str/split scope #" ")}]
    (add-to-atom! repo-atom grizzly-entry)))

;; Find entries by their keys (same for both versions)
(defn find-by-bear [repo bear-id]
  (find-by-key repo :bear bear-id))

(defn find-by-grizzly [repo grizzly-id]
  (find-by-key repo :grizzly grizzly-id))

;; Remove entries - version 1: explicit state management
(defn remove-bear! [repo bear-id]
  (delete! repo :bear bear-id))

;; Remove entries - version 2: atom-aware
(defn remove-bear-atom! [repo-atom bear-id]
  (delete-from-atom! repo-atom :bear bear-id))
```

### Example Usage

You can choose between two styles of usage:

#### Style 1: Explicit State Management

This style gives you control over when and how state updates are applied. This is particularly useful when you need to:
- Batch multiple operations together
- Validate changes before applying them
- Ensure atomic updates of multiple entries

```clojure
;; Create a new repository
(def repo (atom (create-bears-repository "bears.nosql")))

;; Example 1: Simple sequential updates
(let [[new-repo _] (store-bear! @repo "winnie" "hundred-acre-wood" "honey read write" 1735689600000)]
  (reset! repo new-repo))

;; Example 2: Batching multiple operations
(let [;; Collect multiple changes without updating the atom
      [repo1 _] (store-bear! @repo "winnie" "woods" "honey" 1000)
      [repo2 _] (store-bear! repo1 "pooh" "woods" "honey" 1000)  ; Note: using repo1, not @repo
      ;; Maybe validate the changes here
      [final-repo _] (store-bear! repo2 "paddington" "london" "marmalade" 2000)]
  ;; Single reset! applies all changes atomically
  (reset! repo final-repo))

;; WARNING: Be careful with out-of-order updates!
;; This is problematic - the second bear will be lost:
(let [[new-repo1 _] (store-bear! @repo "winnie" "woods" "honey" 1000)
      [new-repo2 _] (store-bear! @repo "pooh" "woods" "honey" 1000)]  ; Using @repo again!
  (reset! repo new-repo2)  ; Has only pooh
  (reset! repo new-repo1)) ; Overwrites with only winnie - pooh is lost!
```

#### Style 2: Atom-Aware Functions

This style is more concise and safer for simple operations. Each operation immediately updates the state:

```clojure
;; Create a new repository
(def repo (atom (create-bears-repository "bears.nosql")))

;; Each operation automatically updates the atom
(store-bear-atom! repo "winnie" "hundred-acre-wood" "honey read write" 1735689600000)
(store-bear-atom! repo "pooh" "woods" "honey" 1000)
(store-bear-atom! repo "paddington" "london" "marmalade" 2000)

;; No risk of out-of-order updates or lost changes
```

Choose the style that best fits your needs:
- Use Style 1 (explicit) when you need to:
  - Batch multiple operations together
  - Validate changes before applying them
  - Ensure atomic updates of multiple entries
  - Roll back changes if something goes wrong
- Use Style 2 (atom-aware) when you:
  - Want simpler, more concise code
  - Are happy with immediate state updates
  - Don't need to batch or validate multiple changes
  - Want to avoid common pitfalls like out-of-order updates

## Structure

- `src/org/clojars/luv2c0d3/ndjson_repository/ndjson.clj` - Core NDJSON repository implementation
- `test/org/clojars/luv2c0d3/ndjson_repository/` - Example implementations demonstrating different use cases:
  - `bubas_test.clj` - Example using the repository for bear-related data
  - `client_test.clj` - Example using the repository for client data storage
  - `tokens_test.clj` - Example using the repository for token storage

Each test file demonstrates a different way to use the NDJSON repository, showing how it can be adapted for various data storage needs while maintaining O(1) lookups through in-memory indexing.

## Java Interop

The library provides a Java-friendly API that handles all Clojure-specific concepts (like atoms and immutability) internally. Here's how to use it from Java:

```java
import org.clojars.luv2c0d3.ndjson_repository.NDJsonRepository;
import java.util.Map;
import java.util.HashMap;

public class BearExample {
    public static void main(String[] args) {
        // Create a repository with two primary keys
        String[] primaryKeys = {"bear", "grizzly"};
        NDJsonRepository repo = new NDJsonRepository("bears.ndjson", primaryKeys);

        // Add some entries
        Map<String, Object> bear = new HashMap<>();
        bear.put("bear", "winnie");
        bear.put("client_id", "hundred-acre-wood");
        bear.put("scope", "honey read write");
        bear.put("expires_at", System.currentTimeMillis() + 3600000);
        repo.add(bear);

        Map<String, Object> grizzly = new HashMap<>();
        grizzly.put("grizzly", "grizz");
        grizzly.put("client_id", "we-bare-bears");
        grizzly.put("scope", "ice-cream read");
        repo.add(grizzly);

        // Find entries
        Map<String, Object> foundBear = repo.findByKey("bear", "winnie");
        System.out.println("Found bear: " + foundBear);

        // Delete entries
        repo.delete("bear", "winnie");
    }
}
```

The Java API:
- Handles all atom management internally
- Uses standard Java collections (Map, List, etc.)
- Follows Java naming conventions
- Provides a simple CRUD interface
- Is thread-safe

You can also use the repository in a more structured way with a wrapper class:

```java
public class BearRepository {
    private final NDJsonRepository repo;

    public BearRepository(String filename) {
        this.repo = new NDJsonRepository(filename, new String[]{"bear", "grizzly"});
    }

    public void addBear(String bearId, String clientId, String scope, long expiresAt) {
        Map<String, Object> bear = new HashMap<>();
        bear.put("bear", bearId);
        bear.put("client_id", clientId);
        bear.put("scope", scope);
        bear.put("expires_at", expiresAt);
        repo.add(bear);
    }

    public void addGrizzly(String grizzlyId, String clientId, String scope) {
        Map<String, Object> grizzly = new HashMap<>();
        grizzly.put("grizzly", grizzlyId);
        grizzly.put("client_id", clientId);
        grizzly.put("scope", scope);
        repo.add(grizzly);
    }

    public Map<String, Object> findBear(String bearId) {
        return repo.findByKey("bear", bearId);
    }

    public Map<String, Object> findGrizzly(String grizzlyId) {
        return repo.findByKey("grizzly", grizzlyId);
    }

    public void deleteBear(String bearId) {
        repo.delete("bear", bearId);
    }

    public void deleteGrizzly(String grizzlyId) {
        repo.delete("grizzly", grizzlyId);
    }
}
```

Usage with the wrapper:
```java
BearRepository bears = new BearRepository("bears.ndjson");

// Add entries
bears.addBear("winnie", "hundred-acre-wood", "honey read write", 
              System.currentTimeMillis() + 3600000);
bears.addGrizzly("grizz", "we-bare-bears", "ice-cream read");

// Find entries
Map<String, Object> bear = bears.findBear("winnie");
System.out.println("Found bear: " + bear);

// Delete entries
bears.deleteBear("winnie");
```

The wrapper approach provides:
- Type-safe methods for specific operations
- Domain-specific naming
- Encapsulation of data structure details
- Simpler interface for common operations

## License

Copyright © 2025

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
