# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [Unreleased]

### 0.3.3 - 03-07-2016

- fix: remove scheduler execution limit;
- add interceptor to check does a command comes from authorized source;
- add a few graphics to make landing user more easier;

## 0.3.2 - 30-06-2016

- fix: allow an user to re-use teams key;
- fix: error-handlers require 2arguments;

## 0.3.1 - 30-06-2016

- add the `cve` command that shows vulnerbality information for a language;
- add the `notifications` command to show todays notifications;
- refactored `veyeslack.api` module into `veyeslack.services.versioneye`;
- refactored `veyeslack.models.auth-token` to use normalized values for user-id,team-id, and channel-id;

## 0.3.0 - 29-06-2016

- add notification that posts once a day new releases of followed packages
- refactored commands to follow CQRS pattern
- refactored the `connect` command, which now save api token permanently;
- updated `selmer` from _1.0.4_ to _1.0.7_


