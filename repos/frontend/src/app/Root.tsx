import {Navigate} from 'react-router-dom';
import {getToken} from '../lib/api';

/** 默认路由：已登录进控制台，否则进登录页。支持页走独立路由。 */
export function Root() {
    return <Navigate to={getToken() ? '/console/products' : '/login'} replace/>;
}
