// @flow

const React = require('react');
const { TopPanel } = require('components/panels/TopPanel');
const MessagesUtils = require('components/messages').Utils;
const { GuestProperties } = require('../guest-properties');
const { VirtualizationGuestActionApi } = require('../virtualization-guest-action-api');
const GuestPropertiesUtils = require('../properties/guest-properties-utils');
const GuestNic = require('../properties/guest-nic');
const GuestDisk = require('../properties/guest-disk');
const { hot } = require('react-hot-loader');

declare function t(msg: string, ...args: Array<any>): string;

type Props = {
  host: Object,
};

type State = {
  model: Object,
  isInvalid: boolean,
  messages: Array<string>,
};

class GuestsCreate extends React.Component<Props, State> {
  static getRequestParameterFromModel(model: Object) {
    const nics = GuestPropertiesUtils.getOrderedDevicesFromModel(model, 'network')
      .map(nic => GuestNic.getRequestParams(model, nic));

    // Diff the model with the initial one to avoid changing disks if user hasn't touched them.
    const disks = GuestPropertiesUtils.getOrderedDevicesFromModel(model, 'disk')
      .map(disk => GuestDisk.getRequestParams(model, disk));

    const filteredProps = ['disk', 'network', 'vmType'];
    return Object.assign(
      Object.entries(model).reduce((res, entry) => Object.assign(res,
        filteredProps.every(prop => !entry[0].startsWith(prop)) ? { [entry[0]]: entry[1] } : undefined),
      {}),
      {
        type: model.vmType,
        memory: model.memory * 1024,
      },
      nics.length !== 0 ? { interfaces: nics } : undefined,
      disks.length !== 0 ? { disks } : undefined,
    );
  }

  initModel = () => ({
    memory: 1024,
    vcpu: 1,
    disk0_editable: true,
    disk0_type: 'file',
    disk0_device: 'disk',
    network0_type: 'network',
    graphicsType: 'spice',
  })

  render() {
    return (
      <VirtualizationGuestActionApi
        hostid={this.props.host.id}
        bounce={`/rhn/manager/systems/details/virtualization/guests/${this.props.host.id}`}
      >
        {
          ({
            onAction,
            messages: actionMessages,
          }) => {
            const initialModel = this.initModel();
            const onSubmit = properties => onAction('update', [],
              GuestsCreate.getRequestParameterFromModel(properties));
            const messages = actionMessages
              .filter(item => item)
              .map(item => MessagesUtils.error(item));
            return (
              <TopPanel
                title={t('Create Virtual Machine')}
                icon="fa spacewalk-icon-virtual-guest"
              >
                <GuestProperties
                  host={this.props.host}
                  submitText={t('Create')}
                  submit={onSubmit}
                  messages={messages}
                  initialModel={initialModel}
                />
              </TopPanel>
            );
          }
        }
      </VirtualizationGuestActionApi>
    );
  }
}

GuestsCreate.propTypes = {
};

module.exports = {
  GuestsCreate: hot(module)(GuestsCreate),
};
