// @flow

const React = require('react');
const { isEqual } = require('lodash');
const { TopPanel } = require('components/panels/TopPanel');
const MessagesUtils = require('components/messages').Utils;
const { GuestProperties } = require('../guest-properties');
const GuestPropertiesUtils = require('../properties/guest-properties-utils');
const GuestNic = require('../properties/guest-nic');
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
      return Object.assign({
        memory: definition.maxMemory / 1024,
        vcpu: definition.vcpu.max,
        graphicsType: definition.graphics ? definition.graphics.type : '',
        osType: definition.os.type,
        arch: definition.os.arch,
        vmType: definition.type,
      },
      GuestNic.getModelFromDefinition(definition));
    }
    return {
      memory: 1024,
      vcpu: 1,
      graphicsType: '',
    };
  }

  static getRequestParameterFromModel(model: Object, initialModel: Object) {
    // Diff the model with the initial one to avoid changing nics if user hasn't touched them.
    const initialNicProps = Object.entries(initialModel).filter(entry => entry[0].startsWith('network'));
    const newNicProps = Object.entries(model).filter(entry => entry[0].startsWith('network'));
    const nics = !isEqual(initialNicProps, newNicProps)
      ? GuestPropertiesUtils.getOrderedDevicesFromModel(model, 'network')
        .map(nic => GuestNic.getRequestParams(model, nic))
      : [];

    const nicsParams = nics.length !== 0 ? { interfaces: nics } : undefined;

    return Object.assign(model, {
      memory: model.memory * 1024,
      nicsParams,
    });
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
                  const onSubmit = properties => onAction('update', [this.props.guestUuid],
                    GuestsEdit.getRequestParameterFromModel(properties, initialModel));
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
