(function(){
  'use strict';
  const list=document.querySelector('.more-tabs');
  const tabs=Array.from(list.querySelectorAll('[data-more-tab]'));
  list.setAttribute('role','tablist');
  function select(id,focus=false){
    const chosen=tabs.find(tab=>tab.dataset.moreTab===id)||tabs[0];
    tabs.forEach(tab=>{
      const active=tab===chosen,panel=document.getElementById(tab.dataset.moreTab);
      tab.setAttribute('role','tab');tab.setAttribute('aria-controls',panel.id);
      tab.setAttribute('aria-selected',String(active));tab.tabIndex=active?0:-1;
      panel.setAttribute('role','tabpanel');panel.tabIndex=0;panel.hidden=!active;
    });
    if(focus)chosen.focus();
  }
  tabs.forEach((tab,index)=>{
    tab.addEventListener('click',event=>{event.preventDefault();history.replaceState(null,'',tab.hash);select(tab.dataset.moreTab)});
    tab.addEventListener('keydown',event=>{
      let next;
      if(event.key==='ArrowRight')next=(index+1)%tabs.length;
      if(event.key==='ArrowLeft')next=(index-1+tabs.length)%tabs.length;
      if(event.key==='Home')next=0;
      if(event.key==='End')next=tabs.length-1;
      if(next!==undefined){event.preventDefault();history.replaceState(null,'',tabs[next].hash);select(tabs[next].dataset.moreTab,true)}
    });
  });
  window.addEventListener('hashchange',()=>select(location.hash.slice(1)));
  select(location.hash.slice(1));
})();
