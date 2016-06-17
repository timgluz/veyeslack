# VeyeSlack

VersionEye commands for Slack.

## Usage


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
