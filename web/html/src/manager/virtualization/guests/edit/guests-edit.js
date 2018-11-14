// @flow

const React = require('react');
const { TopPanel } = require('components/panels/TopPanel');
const MessagesUtils = require('components/messages').Utils;
const { GuestProperties } = require('../guest-properties');
const { VirtualizationGuestActionApi } = require('../virtualization-guest-action-api');
const { VirtualizationGuestDefinitionApi } = require('../virtualization-guest-definition-api');

declare function t(msg: string): string;

type Props = {
  host: Object,
  guestUuid: string,
};

class GuestsEdit extends React.Component<Props> {
  static getModelFromDefinition(definition: ?Object) {
    if (definition != null) {
      return {
        memory: definition.maxMemory / 1024,
        vcpu: definition.vcpu.max,
        graphicsType: definition.graphics ? definition.graphics.type : '',
        osType: definition.os.type,
        arch: definition.os.arch,
        vmType: definition.type,
      };
    }
    return {
      memory: 1024,
      vcpu: 1,
      graphicsType: '',
    };
  }

  render() {
    return (
      <VirtualizationGuestDefinitionApi
        hostid={this.props.host.id}
        guestUuid={this.props.guestUuid}
      >
        {
          ({
            definition,
            messages: definitionMessages,
          }) => (
            <VirtualizationGuestActionApi
              hostid={this.props.host.id}
              bounce={`/rhn/manager/systems/details/virtualization/guests/${this.props.host.id}`}
            >
              {
                ({
                  onAction,
                  messages: actionMessages,
                }) => {
                  const onSubmit = (properties) => {
                    const newProperties = Object.assign(properties, { memory: properties.memory * 1024 });
                    return onAction('update', [this.props.guestUuid], newProperties);
                  };
                  const messages = [].concat(definitionMessages, actionMessages)
                    .filter(item => item)
                    .map(item => MessagesUtils.error(item));
                  const guestName = definition !== null ? definition.name : '';
                  return (
                    <TopPanel title={guestName} icon="fa spacewalk-icon-virtual-guest">
                      <GuestProperties
                        host={this.props.host}
                        submitText={t('Update')}
                        submit={onSubmit}
                        messages={messages}
                        initialModel={GuestsEdit.getModelFromDefinition(definition)}
                      />
                    </TopPanel>
                  );
                }
              }
            </VirtualizationGuestActionApi>)
        }
      </VirtualizationGuestDefinitionApi>);
  }
}

module.exports = {
  GuestsEdit,
};
