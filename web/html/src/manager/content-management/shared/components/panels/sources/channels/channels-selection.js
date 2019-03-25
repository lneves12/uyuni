// @flow
import React, {useEffect} from 'react';
import {Loading} from "../../../../../../../components/loading/loading";
import useChannels from "./api/use-channels";
import styles from "./channels-selection.css";
import GroupChannels from "./group-channels";
import {useImmerReducer} from "use-immer";

import type {ActionChannelsSelectionType, FilterType, StateChannelsSelectionType} from "./channels-selection.state";
import {
  getChannelsFiltersAvailableValues,
  initialStateChannelsSelection,
  reducerChannelsSelection
} from "./channels-selection.state"
import type {UseChannelsType} from "./api/use-channels.js"
import {
  getSelectedChannelsIdsInGroup,
  getVisibleChannels,
  isGroupVisible,
  orderBaseChannels
} from "./channels-selection.utils";

type PropsType = {
  initialSelectedIds: Array<string>,
  onChange: Function,
}

const ChannelsSelection = (props: PropsType) => {
  const {isLoading, channelsTree}: UseChannelsType = useChannels();
  const [state, dispatchChannelsSelection] : [StateChannelsSelectionType, (ActionChannelsSelectionType) => void]
    = useImmerReducer(
    (draft, action) => reducerChannelsSelection(draft, action, channelsTree) ,
    initialStateChannelsSelection(props)
  );

  useEffect(() => {
    // set lead base channel as first and notify
    const sortedSelectedChannelsId = state.selectedChannelsIds
      .filter(cId => cId !== state.selectedBaseChannelId);
    sortedSelectedChannelsId.unshift(state.selectedBaseChannelId)
    !isLoading && props.onChange(
      sortedSelectedChannelsId
        .filter(cId => channelsTree.channelsById[cId])
        .map(cId => channelsTree.channelsById[cId])
    );
  }, [state.selectedChannelsIds])

  if (isLoading) {
    return (
      <div className='form-group'>
        <Loading text='Loading..' />
      </div>
    )
  }

  const onSearch = (search: string) => dispatchChannelsSelection({
    type: "search",
    search
  });

  const visibleChannels = getVisibleChannels(channelsTree, state.activeFilters);
  // Order all base channels by id and set the lead base channel as first
  let orderedBaseChannels = orderBaseChannels(channelsTree, state.selectedBaseChannelId);

  return (
    <div>
      <div className='form-group'>
        <label className='col-lg-3 control-label'>
          {t('New Base Channel')}
        </label>
        <div className='col-lg-8'>
          <select
            name='selectedBaseChannel'
            className='form-control'
            value={state.selectedBaseChannelId}
            onChange={event => dispatchChannelsSelection({type: "lead_channel", newBaseId: event.target.value})}
          >
            <option></option>
            {
              orderedBaseChannels.map(b => <option key={b.id} value={b.id}>{b.name}</option>)
            }
          </select>
          <span className='help-block'>
            {t("Choose the channel to be elected as the new base channel")}
          </span>
        </div>
      </div>
      {
        state.selectedBaseChannelId &&
        <div className='form-group'>
          <label className='col-lg-3 control-label'>
            <div className="row" style={{marginBottom: "30px"}}>
              {`${t('Child Channels')} (${state.selectedChannelsIds.length})`}
            </div>
            <div className="row panel panel-default panel-body text-left">
              <div style={{position: "relative"}}>
                <input
                  type="text"
                  className="form-control"
                  placeholder="Search a channel"
                  value={state.search}
                  onChange={event => onSearch(event.target.value)} />
                <span className={`${styles.search_icon_container} clear`}>
                <i
                  onClick={() => onSearch("")}
                  className="fa fa-times-circle-o no-margin"
                  title={t('Clear Search')}
                />
              </span>
              </div>
              <hr/>
              {
                getChannelsFiltersAvailableValues().map((filter: FilterType)  =>
                  <div key={filter.id} className='checkbox'>
                    <input type='checkbox'
                           value={filter.id}
                           checked={state.activeFilters.includes(filter.id)}
                           id={`filter_${filter.id}`}
                           onChange={(event) => dispatchChannelsSelection({
                             type: "toggle_filter",
                             filter: event.target.value
                           })}
                    />
                    <label htmlFor={`filter_${filter.id}`}>
                      {filter.text}
                    </label>
                  </div>
                )
              }
            </div>
          </label>
          <div className='col-lg-8'>
            <div>
              {
                orderedBaseChannels.map(baseChannel => {
                  const selectedChannelsIdsInGroup = getSelectedChannelsIdsInGroup(state.selectedChannelsIds, baseChannel);

                  if(!isGroupVisible(
                    baseChannel,
                    channelsTree,
                    visibleChannels,
                    selectedChannelsIdsInGroup,
                    state.selectedBaseChannelId,
                    state.search)
                  ) {
                    return null;
                  }

                  const isOpen = state.openGroupsIds
                    .some(openId => openId === baseChannel.id || baseChannel.children.includes(openId));

                  return (
                    <GroupChannels
                      key={`group_${baseChannel.id}`}
                      base={baseChannel}
                      search={state.search}
                      childChannelsId={baseChannel.children}
                      selectedChannelsIdsInGroup={selectedChannelsIdsInGroup}
                      selectedBaseChannelId={state.selectedBaseChannelId}
                      isOpen={isOpen}
                      setAllRecommentedChannels={enable => {
                        dispatchChannelsSelection({
                          type: "set_recommended",
                          baseId: baseChannel.id,
                          enable
                        })
                      }}
                      onChannelsToggle={channelsIds => dispatchChannelsSelection({
                        type: "toggle_channels",
                        baseId: baseChannel.id,
                        channelsIds,
                      })}
                      onOpenGroup={open => dispatchChannelsSelection({
                        type: "open_group",
                        baseId: baseChannel.id,
                        open
                      })}
                      channelsTree={channelsTree}
                    />
                  )
                })
              }
            </div>
          </div>
        </div>
      }
    </div>
  );
};

export default ChannelsSelection;
