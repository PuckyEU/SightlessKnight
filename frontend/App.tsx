import {router} from "Frontend/routes.js";
import {RouterProvider} from "react-router-dom";
import ApplicationLayout from "Frontend/ApplicationLayout";

export default function App() {
  return (
      <ApplicationLayout children={<RouterProvider router={router}/>}/>
  );
}
