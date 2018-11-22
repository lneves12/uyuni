// @flow

const React = require('react');
const { Panel } = require('components/panels/Panel');
const { Text } = require('components/input/Text');
const Validation = require('components/validation');
const MessagesUtils = require('components/messages').Utils;
const { Loading } = require('components/loading/loading');
const { GuestPropertiesForm } = require('./properties/guest-properties-form');
const { GuestPropertiesTraditional } = require('./properties/guest-properties-traditional');
const { VirtualizationDomainsCapsApi } = require('./virtualization-domains-caps-api');

declare function t(msg: string, ...args: Array<any>): string;

type Props = {
  host: Object,
  submitText: string,
  submit: Function,
  initialModel: Object,
  messages: Array<String>,
};

/*
 * Component editing a virtual machine properties
 */
class GuestProperties extends React.Component<Props> {
  validationChecks = [{
    check: (model: Object) => !Number.isNaN(Number.parseInt(model.vcpu, 10))
      && (model.vcpu > this.props.host.cpu.count),
    message: MessagesUtils.warning('Overcommitting CPU can harm performances.'),
  }]

  render() {
    if (!this.props.host.saltEntitled) {
      return (
        <GuestPropertiesTraditional
          host={this.props.host}
          submitText={this.props.submitText}
          submit={this.props.submit}
          initialModel={this.props.initialModel}
          messages={this.props.messages}
        />
      );
    }

    return (
      <VirtualizationDomainsCapsApi hostId={this.props.host.id}>
        {
          ({
            domainsCaps,
            messages,
          }) => {
            const allMessages = [].concat(this.props.messages, messages).filter(item => item);

            if (domainsCaps.length > 0 && allMessages.length === 0) {
              return (
                <GuestPropertiesForm
                  submitText={this.props.submitText}
                  submit={this.props.submit}
                  initialModel={this.props.initialModel}
                  validationChecks={this.validationChecks}
                  messages={allMessages}
                >
                  {
                    ({ model }) => {
                      const vmTypes = domainsCaps.map(cap => cap.domain)
                        .filter((vmType, idx, array) => array.indexOf(vmType) === idx);
                      const vmType = model.vmType
                        || this.props.initialModel.vmType
                        || (vmTypes.includes('kvm') ? 'kvm' : vmTypes[0]);
                      const arch = this.props.initialModel.arch || this.props.host.cpu.arch;
                      const caps = domainsCaps.find(cap => cap.arch === arch && cap.domain === vmType);

                      return [
                        <Panel key="general" title={t('General')} headingLevel="h2">
                          <Text
                            name="memory"
                            label={t('Maximum Memory (MiB)')}
                            required
                            invalidHint={t('A positive integer is required')}
                            labelClass="col-md-3"
                            divClass="col-md-6"
                            validators={[Validation.isInt({ gt: 0 })]}
                          />
                          <Text
                            name="vcpu"
                            label={t('Virtual CPU Count')}
                            required
                            invalidHint={t('A positive integer is required')}
                            labelClass="col-md-3"
                            divClass="col-md-6"
                            validators={[Validation.isInt({ gt: 0 })]}
                          />
                        </Panel>,
                      ];
                    }
                  }
                </GuestPropertiesForm>
              );
            }
            return <Loading text={t('Loading...')} withBorders={false} />;
          }
        }
      </VirtualizationDomainsCapsApi>
    );
  }
}

module.exports = {
  GuestProperties,
};
