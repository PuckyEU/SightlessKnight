import GameView from "Frontend/views/GameView";
import {createBrowserRouter, RouteObject} from "react-router-dom";
import {serverSideRoutes} from "Frontend/generated/flow/Flow";

export const routes: readonly RouteObject[] = [
    {path: "/", element: <GameView/>},
    {path: "/play", element: <GameView/>},
    {path: "/train", element: <GameView train/>},
    {path: "/train/:id", element: <GameView train/>},
    ...serverSideRoutes];

export const router = createBrowserRouter([...routes], {basename: new URL(document.baseURI).pathname});
