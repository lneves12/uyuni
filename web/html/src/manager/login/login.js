const React = require("react");
const Messages = require("../../components/messages").Messages;
const {AsyncButton} = require("../../components/buttons");

import LoginApi from "../../shared/core/api/login-api";

class Login extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      login: "",
      password: "",
    };
  }

  loginChanged = (event) => {
    this.setState({
      login: event.target.value
    });
  };

  passwordChanged = (event) => {
    this.setState({
      password: event.target.value
    });
  };

  renderMessages = ({success, messages}) => {
    if (success) {
      return <Messages items={[{severity: "success", text:
          <p>{t('Successfully logged in! Redirecting...')}.</p>
      }]} />;
    }

    if (messages.length > 0) {
      return  <Messages items={messages.map((msg) => ({ severity: "error", text: msg }) )} />;
    }

    return (null);
  };

  render() {

    return (
      <LoginApi bounce={this.props.bounce}>
        {
          ({
             onLogin,
             success,
             loading,
             messages,
           }) => (
              <div className="col-sm-5 col-sm-offset-1">
                {
                  this.renderMessages({success, messages})
                }
                <h2 className="Raleway-font gray-text">{t("Sign In")}</h2>
                <div className="margins-updown">
                  <input name="login" className="form-control" type="text" placeholder={t("Login")}
                         value={this.state.login} onChange={this.loginChanged}/>
                  <input name="password" className="form-control" type="password" placeholder={t("Password")}
                         value={this.state.password} onChange={this.passwordChanged}/>
                  <AsyncButton
                    id="login-btn"
                    defaultType="btn-success btn-block"
                    name={t("Sign In")}
                    action={() => onLogin({login: this.state.login, password: this.state.password})}/>
                </div>
              </div>
            )
        }
      </LoginApi>
    )
  }
}

LoginApi.propTypes = {
  bounce: React.PropTypes.string.isRequired,
};

export default Login;
