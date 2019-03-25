// @flow
import useMandatoryChannelsApi from "./use-mandatory-channels-api";

type Props = {
  children: Function,
}

// Just a wrapper to use the react-hook useMandatoryChannelsApi in old Code using classes as render-props.
function MandatoryChannelsApi({ children }: Props) {
  let apiHookReturn = useMandatoryChannelsApi()
  return children(apiHookReturn)
}


export default MandatoryChannelsApi;
