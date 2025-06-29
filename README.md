# NDJSON Repository

A Clojure library for NDJSON-based repositories with O(1) lookups through in-memory indexing.

## Structure

- `src/org/clojars/luv2c0d3/ndjson_repository/ndjson.clj` - Core NDJSON repository implementation
- `test/org/clojars/luv2c0d3/ndjson_repository/` - Example implementations demonstrating different use cases:
  - `bubas_test.clj` - Example using the repository for bear-related data
  - `client_test.clj` - Example using the repository for client data storage
  - `tokens_test.clj` - Example using the repository for token storage

Each test file demonstrates a different way to use the NDJSON repository, showing how it can be adapted for various data storage needs while maintaining O(1) lookups through in-memory indexing.

## Installation

Download from http://example.com/FIXME.

## Usage

FIXME: explanation

    $ java -jar mi-app-clj-0.1.0-standalone.jar [args]

## Options

FIXME: listing of options this app accepts.

## Examples

...

### Bugs

...

### Any Other Sections
### That You Think
### Might be Useful

## License

Copyright © 2025 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
