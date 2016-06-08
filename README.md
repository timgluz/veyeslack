# VeyeSlack

VersionEye commands for Slack.

## Usage

<a href="https://slack.com/oauth/authorize?scope=incoming-webhook,commands&client_id=43911260647.48800449890"><img alt="Add to Slack" height="40" width="139" src="https://platform.slack-edge.com/img/add_to_slack.png" srcset="https://platform.slack-edge.com/img/add_to_slack.png 1x, https://platform.slack-edge.com/img/add_to_slack@2x.png 2x" /></a>


[![Slack Button](https://platform.slack-edge.com/img/add_to_slack.png)](https://slack.com/oauth/authorize?scope=incoming-webhook,commands&client_id=43911260647.48800449890)

#### Developers

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
$> ansible-playbook configure.yml -i hosts --user=root

```

###### deploying new release

* build new uberjar

```
$> lein uberjar 
```

* update version in `playbooks/deploy.yml`

* run deployment script

```
$> ansible-playbook deploy.yml -i hosts --user=root
```


## License

Copyright © 2016 VersionEye

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
