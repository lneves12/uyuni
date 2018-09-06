const React = require("react");
const Network = require("../../../utils/network");

class LoginApi extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      messages: [],
      success: false,
      loading: false
    };
  }

  componentDidMount() {
    window.addEventListener("beforeunload", (e) => {
      if (this.state.loading) {
        var confirmationMessage = "Are you sure you want to close this page while login is in progress?";
        (e || window.event).returnValue = confirmationMessage;
        return confirmationMessage;
      }
    })
  }

  onLogin = ({login, password}) => {
    this.setState({loading: true});

    const formData = {
      login: login.trim(),
      password: password.trim()
    };

    const request = Network.post(
      "/rhn/manager/api/login",
      JSON.stringify(formData),
      "application/json"
    ).promise.then(data => {
      this.setState({
        success: data.success,
        messages: data.messages,
        loading: false
      });

      // Redirect in case of success
      if (this.state.success) {
        window.location.replace(this.props.bounce);
      }
    }, (xhr) => {
      try {
        this.setState({
          success: false,
          messages: [JSON.parse(xhr.responseText)],
          loading: false
        })
      } catch (err) {
        const errMessages = xhr.status === 0 ?
          [t("Request interrupted or invalid response received from the server. Please try again.")]
          : [Network.errorMessageByStatus(xhr.status)];
        this.setState({
          success: false,
          messages: errMessages,
          loading: false
        })
      }
    });
    return request;
  };

  render() {
    return this.props.children({
      onLogin: this.onLogin,
      success: this.state.success,
      loading: this.state.loading,
      messages: this.state.messages,
    })
  }
}

LoginApi.propTypes = {
  bounce: React.PropTypes.string.isRequired,
  children: React.PropTypes.func.isRequired,
};

export default LoginApi;
