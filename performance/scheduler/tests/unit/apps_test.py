import falcon
from falcon import testing
import pytest
import json
from datetime import datetime, date

import app
from app import api
from unittest.mock import mock_open, call, patch


def alchemyencoder(obj):
    """JSON encoder function for SQLAlchemy special classes."""
    if isinstance(obj, (datetime, date)):
        return obj.isoformat()
    elif isinstance(obj, decimal.Decimal):
        return float(obj)

@pytest.fixture
def client():
    return testing.TestClient(api)

def test_list_runs_empty(client):
    run_repository_patch = patch(
        'repositories.runs.RunRepository')
    run_repository_mock = run_repository_patch.start()

    run_repository_mock.return_value.get_run_list.return_value = []


    response = client.simulate_get('/runs')

    assert response.text == '[]'
    assert response.status == falcon.HTTP_OK

def test_list_runs(client):
    run_repository_patch = patch(
        'repositories.runs.RunRepository')
    run_repository_mock = run_repository_patch.start()

    run_repository_mock.return_value.get_run_list.return_value = [{
        "id": 1,
        "status": "NotStarted"
    }]

    response = client.simulate_get('/runs')

    assert response.text == json.dumps([{"status": "NotStarted", "id": 1}])
    assert response.status == falcon.HTTP_OK

def test_specific_run(client):
    run_repository_patch = patch(
        'repositories.runs.RunRepository')
    run_repository_mock = run_repository_patch.start()

    run_repository_mock.return_value.get_run.return_value = {
        "id": 1,
        "status": "NotStarted"
    }

    response = client.simulate_get('/runs/1')

    assert response.text == json.dumps({"status": "NotStarted", "id": 1})
    assert response.status == falcon.HTTP_OK

def test_specific_run_does_not_exist(client):
    run_repository_patch = patch(
        'repositories.runs.RunRepository')
    run_repository_mock = run_repository_patch.start()

    run_repository_mock.return_value.get_run.return_value = None

    response = client.simulate_get('/runs/1')

    assert response.text == json.dumps({"result": "No runs found"})
    assert response.status == falcon.HTTP_NOT_FOUND

def test_schedule_run(client):
    run_repository_patch = patch(
        'repositories.runs.RunRepository')
    run_repository_mock = run_repository_patch.start()

    run_repository_mock.return_value.schedule_run.return_value = {
        "id": 1,
        "status": "NotStarted"
    }

    fake_start_run_bytes = b'{"run_names":["test"],"experiment_id":1}'
    response = client.simulate_post(
        '/runs',
        body=fake_start_run_bytes,
        headers={'content-type': 'application/json'}
    )

    assert response.status == falcon.HTTP_CREATED
    assert run_repository_mock.return_value.schedule_run.call_count == 1
    assert response.text == json.dumps({"status": "NotStarted", "id": 1})

def test_schedule_run_no_experiment(client):
    run_repository_patch = patch(
        'repositories.runs.RunRepository')
    run_repository_mock = run_repository_patch.start()

    run_repository_mock.return_value.schedule_run.return_value = None

    # When the service receives an image through POST...
    fake_start_run_bytes = b'{"run_names":["test"],"experiment_id":-1}'
    response = client.simulate_post(
        '/runs',
        body=fake_start_run_bytes,
        headers={'content-type': 'application/json'}
    )

    # ...it must return a 201 code, save the file, and return the
    # image's resource location.
    assert response.status == falcon.HTTP_INTERNAL_SERVER_ERROR
    assert run_repository_mock.return_value.schedule_run.call_count == 1
    assert response.text == json.dumps({"result": "We could not start experiment -1"})

def test_start_run(client):
    run_repository_patch = patch(
        'repositories.runs.RunRepository')
    run_repository_mock = run_repository_patch.start()

    run_repository_mock.return_value.update_run.return_value = {
        "id": 1,
        "status": "Running"
    }

    response = client.simulate_get('/runs/1/actions/start')

    assert response.status == falcon.HTTP_ACCEPTED
    assert run_repository_mock.return_value.update_run.call_count == 1
    assert response.text == json.dumps({"status": "Running", "id": 1})

def test_start_run_does_not_exist(client):
    run_repository_patch = patch(
        'repositories.runs.RunRepository')
    run_repository_mock = run_repository_patch.start()

    run_repository_mock.return_value.update_run.return_value = None

    response = client.simulate_get('/runs/1/actions/start')

    assert response.status == falcon.HTTP_INTERNAL_SERVER_ERROR
    assert run_repository_mock.return_value.update_run.call_count == 1
    assert response.text == json.dumps({"result": "Unable to start 1"})

def test_stop_run(client):
    run_repository_patch = patch(
        'repositories.runs.RunRepository')
    run_repository_mock = run_repository_patch.start()

    run_repository_mock.return_value.update_run.return_value = {
        "id": 1,
        "status": "Completed"
    }

    response = client.simulate_get('/runs/1/actions/stop')

    assert response.status == falcon.HTTP_ACCEPTED
    assert run_repository_mock.return_value.update_run.call_count == 1
    assert response.text == json.dumps({"status": "Completed", "id": 1})

def test_stop_run_does_not_exist(client):
    run_repository_patch = patch(
        'repositories.runs.RunRepository')
    run_repository_mock = run_repository_patch.start()

    run_repository_mock.return_value.update_run.return_value = None

    response = client.simulate_get('/runs/1/actions/stop')

    assert response.status == falcon.HTTP_INTERNAL_SERVER_ERROR
    assert run_repository_mock.return_value.update_run.call_count == 1
    assert response.text == json.dumps({"result": "Unable to stop 1"})

def test_invalid_action_run(client):
    run_repository_patch = patch(
        'repositories.runs.RunRepository')
    run_repository_mock = run_repository_patch.start()

    run_repository_mock.return_value.update_run.return_value = None

    response = client.simulate_get('/runs/1/actions/invalid')

    assert response.status == falcon.HTTP_BAD_REQUEST
    assert run_repository_mock.return_value.update_run.call_count == 0
    assert response.text == json.dumps({"results": "Invalid action: invalid"})

def test_list_experiments(client):
    experiments_repository_patch = patch(
        'repositories.experiments.ExperimentRepository')
    experiments_repository_mock = experiments_repository_patch.start()

    experiment_list = [
        {
            "treatments": [
                {
                    "runs": [],
                    "values": [],
                    "id": 1
                }
            ],
            "name": "test3",
            "id": 1
        }
    ]

    experiments_repository_mock.return_value.get_experiment_list.return_value = experiment_list

    response = client.simulate_get('/experiments')

    assert response.text == json.dumps(experiment_list)
    assert response.status == falcon.HTTP_OK


def test_list_experiments_empty(client):
    experiments_repository_patch = patch(
        'repositories.experiments.ExperimentRepository')
    experiments_repository_mock = experiments_repository_patch.start()

    experiment_list = []

    experiments_repository_mock.return_value.get_experiment_list.return_value = experiment_list

    response = client.simulate_get('/experiments')

    assert response.text == json.dumps(experiment_list)
    assert response.status == falcon.HTTP_OK

def test_get_experiment(client):
    experiments_repository_patch = patch(
        'repositories.experiments.ExperimentRepository')
    experiments_repository_mock = experiments_repository_patch.start()

    experiment = {
        "treatments": [
            {
                "runs": [],
                "values": [],
                "id": 1
            }
        ],
        "name": "test3",
        "id": 1
    }

    experiments_repository_mock.return_value.get_experiment.return_value = experiment

    response = client.simulate_get('/experiments/1')

    assert response.text == json.dumps(experiment)
    assert response.status == falcon.HTTP_OK

def test_get_experiment_does_not_exist(client):
    experiments_repository_patch = patch(
        'repositories.experiments.ExperimentRepository')
    experiments_repository_mock = experiments_repository_patch.start()

    experiment = None

    experiments_repository_mock.return_value.get_experiment.return_value = experiment

    response = client.simulate_get('/experiments/1')

    assert response.text == json.dumps({"result": "No experiments found"})
    assert response.status == falcon.HTTP_NOT_FOUND

def test_create_experiment(client):
    experiments_repository_patch = patch(
        'repositories.experiments.ExperimentRepository')
    experiments_repository_mock = experiments_repository_patch.start()

    experiment = {
        "treatments": [
            {
                "runs": [],
                "values": [
                    {
                        "value": "0",
                        "id": 1,
                        "type": "Average",
                        "name": "soa.v20_create_user_internal.users_per_sec"
                    }, 
                    {
                        "value": "http://192.168.99.101:8082/idm/cloud",
                        "id": 2,
                        "type": "Average",
                        "name": "main_external_auth_url"
                    }
                ],
                "id": 1
            }
        ],
        "name": "test3",
        "id": 1
    }

    experiments_repository_mock.return_value.add_experiment.return_value = experiment

    fake_create_experiment_bytes = b'soa.v20_create_user_internal.users_per_sec=0\nmain_external_auth_url=http://192.168.99.101:8082/idm/cloud\n'
    response = client.simulate_post(
        '/experiments/test',
        query_string='simulation=test.simulation',
        body=fake_create_experiment_bytes
    )

    assert response.text == json.dumps(experiment)
    assert response.status == falcon.HTTP_CREATED
    assert experiments_repository_mock.return_value.add_experiment.call_count == 1

def test_create_experiment_empty_props(client):
    experiments_repository_patch = patch(
        'repositories.experiments.ExperimentRepository')
    experiments_repository_mock = experiments_repository_patch.start()

    experiment = {
        "treatments": [
            {
                "runs": [],
                "values": [],
                "id": 1
            }
        ],
        "name": "test3",
        "id": 1
    }

    experiments_repository_mock.return_value.add_experiment.return_value = experiment

    fake_create_experiment_bytes = b''
    response = client.simulate_post(
        '/experiments/test',
        query_string='simulation=test.simulation',
        body=fake_create_experiment_bytes
    )

    assert response.text == json.dumps(experiment)
    assert response.status == falcon.HTTP_CREATED
    assert experiments_repository_mock.return_value.add_experiment.call_count == 1

def test_create_experiment_duplicate_name(client):
    experiments_repository_patch = patch(
        'repositories.experiments.ExperimentRepository')
    experiments_repository_mock = experiments_repository_patch.start()

    experiment = None

    experiments_repository_mock.return_value.add_experiment.return_value = experiment

    fake_create_experiment_bytes = b''
    response = client.simulate_post(
        '/experiments/test',
        query_string='simulation=test.simulation',
        body=fake_create_experiment_bytes
    )

    assert response.text == json.dumps({"result": "Experiment test already exists"})
    assert response.status == falcon.HTTP_409
    assert experiments_repository_mock.return_value.add_experiment.call_count == 1

def test_create_experiment_no_simulation(client):
    experiments_repository_patch = patch(
        'repositories.experiments.ExperimentRepository')
    experiments_repository_mock = experiments_repository_patch.start()

    experiments_repository_mock.return_value.add_experiment.return_value = None

    fake_create_experiment_bytes = b'stuffhere'
    response = client.simulate_post(
        '/experiments/test',
        body=fake_create_experiment_bytes
    )

    assert response.status == falcon.HTTP_BAD_REQUEST
    assert experiments_repository_mock.return_value.add_experiment.call_count == 0

def test_list_agents_empty(client):
    agent_repository_patch = patch(
        'repositories.agents.MachineRepository')
    agent_repository_mock = agent_repository_patch.start()

    agent_repository_mock.return_value.list_machines.return_value = []


    response = client.simulate_get('/agents')

    assert response.text == '[]'
    assert response.status == falcon.HTTP_OK
    assert agent_repository_mock.return_value.list_machines.call_count == 1

def test_list_agents(client):
    agent_repository_patch = patch(
        'repositories.agents.MachineRepository')
    agent_repository_mock = agent_repository_patch.start()

    result = [{
        "id": 1,
        "machine_directory": "/path/to/gatling",
        "address": "192.168.1.12"
    }]

    agent_repository_mock.return_value.list_machines.return_value = result

    response = client.simulate_get('/agents')

    assert response.text == json.dumps(result)
    assert response.status == falcon.HTTP_OK
    assert agent_repository_mock.return_value.list_machines.call_count == 1

def test_specific_agent(client):
    agent_repository_patch = patch(
        'repositories.agents.MachineRepository')
    agent_repository_mock = agent_repository_patch.start()

    agent_repository_mock.return_value.get_machine.return_value = {
        "id": 1,
        "machine_directory": "/path/to/gatling",
        "address": "192.168.1.12"
    }

    response = client.simulate_get('/agents/1')

    assert response.text == json.dumps({
        "id": 1,
        "machine_directory": "/path/to/gatling",
        "address": "192.168.1.12"
    })
    assert response.status == falcon.HTTP_OK
    assert agent_repository_mock.return_value.get_machine.call_count == 1

def test_specific_agent_does_not_exist(client):
    agent_repository_patch = patch(
        'repositories.agents.MachineRepository')
    agent_repository_mock = agent_repository_patch.start()

    agent_repository_mock.return_value.get_machine.return_value = None

    response = client.simulate_get('/agents/1')

    assert response.text == json.dumps({"result": "No agents found"})
    assert response.status == falcon.HTTP_NOT_FOUND
    assert agent_repository_mock.return_value.get_machine.call_count == 1

def test_register_agent(client):
    agent_repository_patch = patch(
        'repositories.agents.MachineRepository')
    agent_repository_mock = agent_repository_patch.start()

    current_time = datetime.now()

    agent_repository_mock.return_value.add_machine.return_value = {
        "main_directory": "/path/to/gatling",
        "agent_address": "192.168.1.1",
        "registered": current_time
    }

    fake_register_agent_bytes = b'{"main_directory": "/path/to/gatling","agent_address": "192.168.1.1"}'
    response = client.simulate_post(
        '/agents',
        body=fake_register_agent_bytes,
        headers={'content-type': 'application/json'}
    )

    assert response.status == falcon.HTTP_CREATED
    assert agent_repository_mock.return_value.add_machine.call_count == 1
    assert response.text == json.dumps({"main_directory": "/path/to/gatling","agent_address": "192.168.1.1", "registered": current_time}, default=alchemyencoder)

def test_register_agent_does_not_exist(client):
    agent_repository_patch = patch(
        'repositories.agents.MachineRepository')
    agent_repository_mock = agent_repository_patch.start()

    agent_repository_mock.return_value.add_machine.return_value = None

    fake_register_agent_bytes = b'{"main_directory": "/path/to/gatling","agent_address": "192.168.1.1"}'
    response = client.simulate_post(
        '/agents',
        body=fake_register_agent_bytes,
        headers={'content-type': 'application/json'}
    )

    assert response.status == falcon.HTTP_INTERNAL_SERVER_ERROR
    assert agent_repository_mock.return_value.add_machine.call_count == 1

def test_register_agent_no_agent_address(client):
    agent_repository_patch = patch(
        'repositories.agents.MachineRepository')
    agent_repository_mock = agent_repository_patch.start()

    agent_repository_mock.return_value.add_machine.return_value = None

    fake_register_agent_bytes = b'{"main_directory": "/path/to/gatling"}'
    response = client.simulate_post(
        '/agents',
        body=fake_register_agent_bytes,
        headers={'content-type': 'application/json'}
    )

    assert response.status == falcon.HTTP_BAD_REQUEST
    assert agent_repository_mock.return_value.add_machine.call_count == 0

def test_register_agent_no_main_directory(client):
    agent_repository_patch = patch(
        'repositories.agents.MachineRepository')
    agent_repository_mock = agent_repository_patch.start()

    agent_repository_mock.return_value.add_machine.return_value = None

    fake_register_agent_bytes = b'{"agent_address": "192.168.1.1"}'
    response = client.simulate_post(
        '/agents',
        body=fake_register_agent_bytes,
        headers={'content-type': 'application/json'}
    )

    assert response.status == falcon.HTTP_BAD_REQUEST
    assert agent_repository_mock.return_value.add_machine.call_count == 0


def test_deregister_agent(client):
    agent_repository_patch = patch(
        'repositories.agents.MachineRepository')
    agent_repository_mock = agent_repository_patch.start()

    current_time = datetime.now()

    agent_repository_mock.return_value.delete_machine.return_value = True

    response = client.simulate_delete('/agents/1')

    assert response.status == falcon.HTTP_NO_CONTENT
    assert agent_repository_mock.return_value.delete_machine.call_count == 1

def test_deregister_agent_does_not_exist(client):
    agent_repository_patch = patch(
        'repositories.agents.MachineRepository')
    agent_repository_mock = agent_repository_patch.start()

    agent_repository_mock.return_value.delete_machine.return_value = False

    response = client.simulate_delete('/agents/1')

    assert response.status == falcon.HTTP_INTERNAL_SERVER_ERROR
    assert agent_repository_mock.return_value.delete_machine.call_count == 1
