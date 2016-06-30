# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [Unreleased]

- add the `cve` command that shows vulnerbality information for a language;
- add the `notifications` command to show todays notifications;
- refactored `veyeslack.api` module into `veyeslack.services.versioneye`;
- refactored `veyeslack.models.auth-token` to use normalized values for user-id,team-id, and channel-id;

## 0.3.0

- add notification that posts once a day new releases of followed packages
- refactored commands to follow CQRS pattern
- refactored the `connect` command, which now save api token permanently;
- updated `selmer` from _1.0.4_ to _1.0.7_


