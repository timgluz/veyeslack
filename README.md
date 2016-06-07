# VeyeSlack

VersionEye commands for Slack.

## Usage

[TODO: here will be Slack button]

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
