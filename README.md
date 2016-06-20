# VeyeSlack

VersionEye commands for Slack.

## Usage


<a href="https://slack.com/oauth/authorize?scope=incoming-webhook,commands&client_id=43911260647.48800449890"><img alt="Add to Slack" height="40" width="139" src="https://platform.slack-edge.com/img/add_to_slack.png" srcset="https://platform.slack-edge.com/img/add_to_slack.png 1x, https://platform.slack-edge.com/img/add_to_slack@2x.png 2x" /></a>

#### Developers
|    Dependencies					    |    Build                   |
|:----------------------------------:|:---------------------------:|
|[![Dependency Status](https://www.versioneye.com/user/projects/5767c9a0fdabcd003d0866c2/badge.svg?style=flat)](https://www.versioneye.com/user/projects/5767c9a0fdabcd003d0866c2)|[![CircleCI](https://circleci.com/gh/timgluz/veyeslack.svg?style=svg)](https://circleci.com/gh/timgluz/veyeslack)|

* run server

```
lein run
```

* run tests

```
lein test
```

#### Devops tasks

###### initializing a new server

```
$> cd playbooks
$> ansible-playbook install.yml -i hosts --user=root

# update settings & restart services
$> ansible-playbook configure.yml -i hosts -e @vars/production.yml

# initialize database
$> ansible-playbook init_postgres.yml -i hosts --become-user=postgres

# run DB migrations from dev machine to instance

```

###### deploying new release

* build new uberjar

```
$> lein uberjar 
```

* update version in `playbooks/deploy.yml`

* update environment variables for the APP

```
$> ansible-playbook update_env.yml -i hosts -e @vars/production.yml
```

* run deployment script

```
$> ansible-playbook deploy.yml -i hosts --user=root
```


## License

Copyright © 2016 VersionEye

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
